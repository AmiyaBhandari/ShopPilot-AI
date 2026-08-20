package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.SaleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ExtractedInvoiceItem(
    val productName: String,
    val quantity: Double,
    val unit: String = "pcs",
    val unitCost: Double,
    val discount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val total: Double = quantity * unitCost,
    val matchedProductId: String? = null,
    val isNewProduct: Boolean = false,
    val confidence: String = "High" // "High", "Needs review"
)

data class ExtractedInvoice(
    val supplierName: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val items: List<ExtractedInvoiceItem>,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val total: Double,
    val notes: String = "",
    val needsReview: Boolean = false
)

sealed class ParsedIntentAction {
    data class RecordSale(
        val items: List<SaleItem>,
        val paymentMethod: PaymentMethod,
        val customerName: String = "",
        val rawSpeech: String = ""
    ) : ParsedIntentAction()

    data class RecordPurchase(
        val supplierName: String,
        val productName: String,
        val quantity: Double,
        val unitCost: Double,
        val rawSpeech: String = ""
    ) : ParsedIntentAction()

    data class AdjustStock(
        val productName: String,
        val newQuantity: Double,
        val reason: String = "Voice Adjustment",
        val rawSpeech: String = ""
    ) : ParsedIntentAction()

    data class CustomerPayment(
        val customerName: String,
        val amount: Double,
        val rawSpeech: String = ""
    ) : ParsedIntentAction()

    data class QueryResponse(
        val answerText: String,
        val dataReasoning: String? = null
    ) : ParsedIntentAction()

    data class Unknown(val message: String) : ParsedIntentAction()
}

class GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val isApiKeyConfigured: Boolean
        get() = try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")
        } catch (e: Exception) {
            false
        }

    private suspend fun callGeminiApi(prompt: String, bitmap: Bitmap? = null): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please set your key in AI Studio secrets.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val partsArray = JSONArray()

        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val imagePart = JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                })
            }
            partsArray.put(imagePart)
        }

        val textPart = JSONObject().apply {
            put("text", prompt)
        }
        partsArray.put(textPart)

        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", partsArray)
            })
        }

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("topP", 0.95)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            Log.e("GeminiAiService", "API Error (${response.code}): $responseBody")
            throw RuntimeException("Gemini API error: ${response.code} ${response.message}")
        }

        val rootJson = JSONObject(responseBody)
        val candidates = rootJson.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw RuntimeException("Empty response from AI")
        }

        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val textResult = parts?.optJSONObject(0)?.optString("text") ?: ""
        textResult
    }

    /**
     * Ask Gemini Assistant with Shop context
     */
    suspend fun askAssistant(userQuery: String, shopContextJson: String, language: String): String {
        val prompt = """
            You are "ShopPilot AI", a friendly, ultra-knowledgeable, and concise business & inventory assistant for a small Indian retail shopkeeper (kirana/general store/wholesaler).
            
            Current Shop Data Context:
            $shopContextJson
            
            Language Preference: $language (Support English, Hindi, or Hinglish as natural for Indian retail shopkeepers).
            Currency: Always use Indian Rupees (₹).
            
            Rules:
            1. Be concise, direct, practical, and respectful.
            2. Ground answers STRICTLY in the provided shop data.
            3. NEVER invent or fabricate numerical figures. If data is unavailable or insufficient, state it honestly.
            4. If recommending purchases or pointing out issues, briefly explain the reason.
            5. Provide clear, bulleted answers with key figures bolded.
            
            Shopkeeper Query: "$userQuery"
        """.trimIndent()

        return callGeminiApi(prompt)
    }

    /**
     * Interpret voice/text shopkeeper intent into structured proposed actions
     */
    suspend fun parseVoiceIntent(userSpeech: String, productsContextJson: String): ParsedIntentAction {
        val prompt = """
            You are the Voice Intent Parser for ShopPilot AI, an Indian retail shop assistant.
            The user spoke or typed in Hindi, Hinglish, or English.
            
            Shopkeeper Speech: "$userSpeech"
            
            Existing Products Catalog in Shop:
            $productsContextJson
            
            Identify the user's intent and output a strictly valid JSON object matching ONE of these structures:
            
            Case 1: Sale Recording (e.g. "Aaj 4 Maggi aur 2 Coke cash me beche", "Sold 3 Parle G for 30 rs upi", "Ramesh ko 2 bread udhar di")
            {
              "action": "SALE",
              "items": [
                { "productName": "Maggi 70g", "quantity": 4, "unitPrice": 14.0 },
                { "productName": "Coca-Cola 500ml", "quantity": 2, "unitPrice": 40.0 }
              ],
              "paymentMethod": "CASH" | "UPI" | "CREDIT" | "BANK",
              "customerName": "Ramesh" (if mentioned, otherwise "")
            }
            
            Case 2: Purchase / Stock Inward (e.g. "ABC Traders se 20 Maggi aayi 10 rs cost pe", "Received 50 Parle-G from Balaji")
            {
              "action": "PURCHASE",
              "supplierName": "ABC Traders",
              "productName": "Maggi 70g",
              "quantity": 20,
              "unitCost": 10.0
            }
            
            Case 3: Customer Payment / Khata (e.g. "Ramesh ne 500 rupaye diye", "Collected 1000 from Suresh")
            {
              "action": "CUSTOMER_PAYMENT",
              "customerName": "Ramesh",
              "amount": 500.0
            }
            
            Case 4: Query / Question (e.g. "Maggi ka stock kitna hai?", "Kaunsa maal khatam hone wala hai?", "Kal kitni sale hui?")
            {
              "action": "QUERY",
              "answer": "Direct concise answer in Hinglish/Hindi/English",
              "reasoning": "Underlying data reference"
            }
            
            Return ONLY raw JSON, no markdown fences if possible, or inside ```json ```.
        """.trimIndent()

        val responseText = callGeminiApi(prompt)
        return try {
            val cleanJson = extractJson(responseText)
            val json = JSONObject(cleanJson)
            when (json.optString("action").uppercase()) {
                "SALE" -> {
                    val itemsArray = json.optJSONArray("items") ?: JSONArray()
                    val saleItems = mutableListOf<SaleItem>()
                    for (i in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(i)
                        val name = itemObj.optString("productName", "Product")
                        val qty = itemObj.optDouble("quantity", 1.0)
                        val price = itemObj.optDouble("unitPrice", 0.0)
                        saleItems.add(
                            SaleItem(
                                productId = "",
                                productName = name,
                                quantity = qty,
                                unitPrice = price,
                                costPrice = price * 0.8,
                                total = qty * price
                            )
                        )
                    }
                    val methodStr = json.optString("paymentMethod", "CASH").uppercase()
                    val method = try { PaymentMethod.valueOf(methodStr) } catch (e: Exception) { PaymentMethod.CASH }
                    ParsedIntentAction.RecordSale(
                        items = saleItems,
                        paymentMethod = method,
                        customerName = json.optString("customerName", ""),
                        rawSpeech = userSpeech
                    )
                }
                "PURCHASE" -> {
                    ParsedIntentAction.RecordPurchase(
                        supplierName = json.optString("supplierName", "General Supplier"),
                        productName = json.optString("productName", "Product"),
                        quantity = json.optDouble("quantity", 1.0),
                        unitCost = json.optDouble("unitCost", 0.0),
                        rawSpeech = userSpeech
                    )
                }
                "CUSTOMER_PAYMENT" -> {
                    ParsedIntentAction.CustomerPayment(
                        customerName = json.optString("customerName", "Customer"),
                        amount = json.optDouble("amount", 0.0),
                        rawSpeech = userSpeech
                    )
                }
                "QUERY" -> {
                    ParsedIntentAction.QueryResponse(
                        answerText = json.optString("answer", "No answer provided."),
                        dataReasoning = json.optString("reasoning", null)
                    )
                }
                else -> ParsedIntentAction.QueryResponse(
                    answerText = responseText,
                    dataReasoning = "Interpreted directly from query"
                )
            }
        } catch (e: Exception) {
            ParsedIntentAction.QueryResponse(
                answerText = responseText,
                dataReasoning = "Direct AI Response"
            )
        }
    }

    /**
     * Scan and extract invoice details from an invoice photo using Multimodal Gemini Vision
     */
    suspend fun extractInvoiceFromImage(bitmap: Bitmap, existingProductsJson: String): ExtractedInvoice {
        val prompt = """
            You are the specialized Retail Invoice OCR and Extraction Engine for ShopPilot AI.
            Analyze this photo of a wholesale/retail invoice, bill, or receipt from an Indian vendor/distributor.
            
            Existing Shop Products catalog:
            $existingProductsJson
            
            Extract the following structured information into a JSON object:
            {
              "supplierName": "Name of distributor/supplier or Wholesale Vendor",
              "invoiceNumber": "Invoice/Bill Number or ''",
              "invoiceDate": "DD/MM/YYYY or YYYY-MM-DD or ''",
              "items": [
                {
                  "productName": "Clean item name",
                  "quantity": 10.0,
                  "unit": "pcs" | "box" | "kg" | "pkt" | "bottle",
                  "unitCost": 45.0,
                  "discount": 0.0,
                  "taxPercent": 5.0,
                  "total": 450.0,
                  "matchedProductId": "Existing product id if exact match found, else null",
                  "isNewProduct": true / false,
                  "confidence": "High" | "Needs review"
                }
              ],
              "subtotal": 450.0,
              "discount": 0.0,
              "tax": 22.5,
              "total": 472.5,
              "notes": "Any payment terms or distributor remarks",
              "needsReview": true if photo was blurry, unclear, or has ambiguous prices/quantities
            }
            
            Important:
            1. If text is blurry or uncertain, mark confidence as "Needs review" and set needsReview = true.
            2. Match items with Existing Products Catalog where relevant.
            3. Return ONLY valid JSON.
        """.trimIndent()

        val responseText = callGeminiApi(prompt, bitmap)
        val cleanJson = extractJson(responseText)
        val json = JSONObject(cleanJson)

        val itemsArray = json.optJSONArray("items") ?: JSONArray()
        val extractedItems = mutableListOf<ExtractedInvoiceItem>()

        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.getJSONObject(i)
            val name = itemObj.optString("productName", "Item #${i + 1}")
            val qty = itemObj.optDouble("quantity", 1.0)
            val unit = itemObj.optString("unit", "pcs")
            val cost = itemObj.optDouble("unitCost", 0.0)
            val discount = itemObj.optDouble("discount", 0.0)
            val tax = itemObj.optDouble("taxPercent", 0.0)
            val lineTotal = itemObj.optDouble("total", qty * cost)
            val matchedId = if (itemObj.has("matchedProductId") && !itemObj.isNull("matchedProductId")) itemObj.getString("matchedProductId") else null
            val isNew = itemObj.optBoolean("isNewProduct", matchedId == null)
            val conf = itemObj.optString("confidence", "High")

            extractedItems.add(
                ExtractedInvoiceItem(
                    productName = name,
                    quantity = qty,
                    unit = unit,
                    unitCost = cost,
                    discount = discount,
                    taxPercent = tax,
                    total = lineTotal,
                    matchedProductId = matchedId,
                    isNewProduct = isNew,
                    confidence = conf
                )
            )
        }

        return ExtractedInvoice(
            supplierName = json.optString("supplierName", "Distributor Invoice"),
            invoiceNumber = json.optString("invoiceNumber", "INV-${System.currentTimeMillis() % 100000}"),
            invoiceDate = json.optString("invoiceDate", "Today"),
            items = extractedItems,
            subtotal = json.optDouble("subtotal", extractedItems.sumOf { it.total }),
            discount = json.optDouble("discount", 0.0),
            tax = json.optDouble("tax", 0.0),
            total = json.optDouble("total", extractedItems.sumOf { it.total }),
            notes = json.optString("notes", ""),
            needsReview = json.optBoolean("needsReview", false)
        )
    }

    private fun extractJson(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        str = str.trim()
        val startIndex = str.indexOf('{')
        val endIndex = str.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return str.substring(startIndex, endIndex + 1)
        }
        return str
    }
}
