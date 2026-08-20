package com.example.data.demo

import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DemoData {

    fun getDemoProducts(): List<ProductEntity> {
        val now = System.currentTimeMillis()
        val day = 86400000L

        return listOf(
            ProductEntity(
                id = "prod_maggi_70",
                name = "Maggi 2-Minute Noodles 70g",
                category = "Snacks & Noodles",
                brand = "Nestle",
                sku = "MAG-70",
                barcode = "8901058852378",
                unit = "pkt",
                costPrice = 11.50,
                sellingPrice = 14.00,
                currentStock = 8.0,
                reservedStock = 0.0,
                minimumStock = 10.0,
                reorderLevel = 25.0,
                supplierId = "sup_abc_traders",
                supplierName = "ABC Traders",
                notes = "Fast moving daily staple. High demand in evenings.",
                createdAt = now - (30 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_parle_g",
                name = "Parle-G Glucose Biscuits 250g",
                category = "Biscuits & Bakery",
                brand = "Parle",
                sku = "PARLE-250",
                barcode = "8901719101019",
                unit = "pkt",
                costPrice = 24.00,
                sellingPrice = 30.00,
                currentStock = 45.0,
                reservedStock = 0.0,
                minimumStock = 15.0,
                reorderLevel = 30.0,
                supplierId = "sup_balaji",
                supplierName = "Balaji Wholesale Agencies",
                notes = "Top seller with morning tea crowd.",
                createdAt = now - (45 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_lays_classic",
                name = "Lay's Classic Salted Chips 50g",
                category = "Snacks & Noodles",
                brand = "PepsiCo",
                sku = "LAYS-50-SALT",
                barcode = "8901491101851",
                unit = "pkt",
                costPrice = 16.00,
                sellingPrice = 20.00,
                currentStock = 32.0,
                reservedStock = 0.0,
                minimumStock = 12.0,
                reorderLevel = 25.0,
                supplierId = "sup_metro_fmcg",
                supplierName = "Metro FMCG Hub",
                createdAt = now - (20 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_coke_500",
                name = "Coca-Cola Cold Drink 500ml",
                category = "Beverages",
                brand = "Coca-Cola",
                sku = "COKE-500",
                barcode = "8901764012236",
                unit = "bottle",
                costPrice = 32.00,
                sellingPrice = 40.00,
                currentStock = 15.0,
                reservedStock = 0.0,
                minimumStock = 10.0,
                reorderLevel = 24.0,
                supplierId = "sup_metro_fmcg",
                supplierName = "Metro FMCG Hub",
                createdAt = now - (25 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_pepsi_500",
                name = "Pepsi Cola 500ml",
                category = "Beverages",
                brand = "PepsiCo",
                sku = "PEPSI-500",
                barcode = "8902080000049",
                unit = "bottle",
                costPrice = 31.50,
                sellingPrice = 40.00,
                currentStock = 18.0,
                reservedStock = 0.0,
                minimumStock = 10.0,
                reorderLevel = 20.0,
                supplierId = "sup_metro_fmcg",
                supplierName = "Metro FMCG Hub",
                createdAt = now - (25 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_surf_excel_1kg",
                name = "Surf Excel Easy Wash Detergent 1kg",
                category = "Household & Cleaning",
                brand = "Hindustan Unilever",
                sku = "SURF-1KG",
                barcode = "8901030382343",
                unit = "pkt",
                costPrice = 125.00,
                sellingPrice = 145.00,
                currentStock = 12.0,
                reservedStock = 0.0,
                minimumStock = 5.0,
                reorderLevel = 10.0,
                supplierId = "sup_balaji",
                supplierName = "Balaji Wholesale Agencies",
                createdAt = now - (60 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_tata_salt_1kg",
                name = "Tata Salt Vacuum Evaporated 1kg",
                category = "Grocery & Staples",
                brand = "Tata",
                sku = "TATA-SALT-1K",
                barcode = "8904043901006",
                unit = "pkt",
                costPrice = 24.00,
                sellingPrice = 28.00,
                currentStock = 50.0,
                reservedStock = 0.0,
                minimumStock = 20.0,
                reorderLevel = 40.0,
                supplierId = "sup_balaji",
                supplierName = "Balaji Wholesale Agencies",
                createdAt = now - (60 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_aashirvaad_atta_5kg",
                name = "Aashirvaad Shudh Chakki Atta 5kg",
                category = "Grocery & Staples",
                brand = "ITC",
                sku = "AASH-ATTA-5K",
                barcode = "8901725181227",
                unit = "bag",
                costPrice = 215.00,
                sellingPrice = 245.00,
                currentStock = 4.0, // Low stock!
                reservedStock = 0.0,
                minimumStock = 8.0,
                reorderLevel = 15.0,
                supplierId = "sup_balaji",
                supplierName = "Balaji Wholesale Agencies",
                notes = "High demand staple. Reorder urgently.",
                createdAt = now - (60 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_amul_milk_500",
                name = "Amul Taaza Homogenised Toned Milk 500ml",
                category = "Dairy",
                brand = "Amul",
                sku = "AMUL-MILK-500",
                barcode = "8901262010052",
                unit = "pkt",
                costPrice = 26.00,
                sellingPrice = 28.00,
                currentStock = 22.0,
                reservedStock = 0.0,
                minimumStock = 15.0,
                reorderLevel = 30.0,
                supplierId = "sup_abc_traders",
                supplierName = "ABC Traders",
                createdAt = now - (15 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_britannia_good_day",
                name = "Britannia Good Day Butter Cookies 200g",
                category = "Biscuits & Bakery",
                brand = "Britannia",
                sku = "BRIT-GD-200",
                barcode = "8901063012019",
                unit = "pkt",
                costPrice = 25.00,
                sellingPrice = 30.00,
                currentStock = 28.0,
                reservedStock = 0.0,
                minimumStock = 10.0,
                reorderLevel = 20.0,
                supplierId = "sup_balaji",
                supplierName = "Balaji Wholesale Agencies",
                createdAt = now - (35 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_fortune_oil_1l",
                name = "Fortune Sunlite Refined Sunflower Oil 1L",
                category = "Grocery & Staples",
                brand = "Fortune",
                sku = "FORT-OIL-1L",
                barcode = "8906007280014",
                unit = "pouch",
                costPrice = 142.00,
                sellingPrice = 165.00,
                currentStock = 7.0,
                reservedStock = 0.0,
                minimumStock = 10.0,
                reorderLevel = 20.0,
                supplierId = "sup_balaji",
                supplierName = "Balaji Wholesale Agencies",
                createdAt = now - (40 * day),
                updatedAt = now
            ),
            ProductEntity(
                id = "prod_vintage_incense",
                name = "Chandan Premium Agarbatti Box",
                category = "Pooja & Household",
                brand = "Sugandh",
                sku = "AGAR-BOX",
                barcode = "8904000100221",
                unit = "box",
                costPrice = 45.00,
                sellingPrice = 70.00,
                currentStock = 25.0, // Dead stock (no sales in 35 days)
                reservedStock = 0.0,
                minimumStock = 5.0,
                reorderLevel = 10.0,
                supplierId = "sup_abc_traders",
                supplierName = "ABC Traders",
                notes = "Slow-moving. Consider festive bundling.",
                createdAt = now - (60 * day),
                updatedAt = now
            )
        )
    }

    fun getDemoSuppliers(): List<SupplierEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            SupplierEntity(
                id = "sup_abc_traders",
                name = "ABC Traders",
                phone = "+91 98201 12345",
                productsSummary = "Nestle, Amul, Spices, Incense",
                outstandingAmount = 3400.0,
                notes = "Delivers every Tuesday and Friday morning.",
                createdAt = now - 90 * 86400000L,
                updatedAt = now
            ),
            SupplierEntity(
                id = "sup_balaji",
                name = "Balaji Wholesale Agencies",
                phone = "+91 98450 54321",
                productsSummary = "Parle, Britannia, ITC Atta, Tata Salt, Surf Excel",
                outstandingAmount = 5200.0,
                notes = "Offers 2% cash discount on same-day settlement.",
                createdAt = now - 120 * 86400000L,
                updatedAt = now
            ),
            SupplierEntity(
                id = "sup_metro_fmcg",
                name = "Metro FMCG Hub",
                phone = "+91 97110 98765",
                productsSummary = "Coca-Cola, PepsiCo, Lay's, Confectionery",
                outstandingAmount = 1800.0,
                notes = "Minimum order value ₹2,000 for free delivery.",
                createdAt = now - 60 * 86400000L,
                updatedAt = now
            )
        )
    }

    fun getDemoCustomers(): List<CustomerEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            CustomerEntity(
                id = "cust_ramesh",
                name = "Ramesh Sharma",
                phone = "+91 98765 43210",
                creditBalance = 2350.0,
                totalPurchases = 14500.0,
                notes = "Regular resident of Flat 302, Green Enclave. Pays on 1st of month.",
                createdAt = now - 100 * 86400000L,
                updatedAt = now
            ),
            CustomerEntity(
                id = "cust_priya",
                name = "Priya Patel",
                phone = "+91 98112 34567",
                creditBalance = 850.0,
                totalPurchases = 8900.0,
                notes = "Buys daily dairy and bread.",
                createdAt = now - 80 * 86400000L,
                updatedAt = now
            ),
            CustomerEntity(
                id = "cust_suresh",
                name = "Suresh Gupta",
                phone = "+91 98990 11223",
                creditBalance = 0.0,
                totalPurchases = 6200.0,
                notes = "Prefers UPI payments always.",
                createdAt = now - 45 * 86400000L,
                updatedAt = now
            ),
            CustomerEntity(
                id = "cust_anita",
                name = "Anita Verma",
                phone = "+91 98234 56789",
                creditBalance = 1200.0,
                totalPurchases = 11400.0,
                notes = "Monthly grocery khata.",
                createdAt = now - 70 * 86400000L,
                updatedAt = now
            )
        )
    }

    fun getDemoSales(): List<SaleEntity> {
        val now = System.currentTimeMillis()
        val hour = 3600000L
        val day = 86400000L

        val sales = mutableListOf<SaleEntity>()

        // Today's Sales
        sales.add(
            createSale(
                id = "sale_today_1",
                items = listOf(
                    SaleItem("prod_maggi_70", "Maggi 2-Minute Noodles 70g", 4.0, 14.0, 11.50, 56.0),
                    SaleItem("prod_coke_500", "Coca-Cola Cold Drink 500ml", 2.0, 40.0, 32.0, 80.0),
                    SaleItem("prod_lays_classic", "Lay's Classic Salted Chips 50g", 3.0, 20.0, 16.0, 60.0)
                ),
                paymentMethod = PaymentMethod.CASH,
                timestamp = now - (2 * hour)
            )
        )

        sales.add(
            createSale(
                id = "sale_today_2",
                items = listOf(
                    SaleItem("prod_aashirvaad_atta_5kg", "Aashirvaad Shudh Chakki Atta 5kg", 1.0, 245.0, 215.0, 245.0),
                    SaleItem("prod_tata_salt_1kg", "Tata Salt Vacuum Evaporated 1kg", 2.0, 28.0, 24.0, 56.0),
                    SaleItem("prod_amul_milk_500", "Amul Taaza Homogenised Toned Milk 500ml", 2.0, 28.0, 26.0, 56.0)
                ),
                paymentMethod = PaymentMethod.UPI,
                timestamp = now - (4 * hour)
            )
        )

        sales.add(
            createSale(
                id = "sale_today_3",
                items = listOf(
                    SaleItem("prod_parle_g", "Parle-G Glucose Biscuits 250g", 3.0, 30.0, 24.0, 90.0),
                    SaleItem("prod_britannia_good_day", "Britannia Good Day Butter Cookies 200g", 2.0, 30.0, 25.0, 60.0)
                ),
                paymentMethod = PaymentMethod.CREDIT,
                customerId = "cust_ramesh",
                customerName = "Ramesh Sharma",
                timestamp = now - (5 * hour)
            )
        )

        // Yesterday & Previous Days Sales
        for (d in 1..7) {
            sales.add(
                createSale(
                    id = "sale_past_${d}_1",
                    items = listOf(
                        SaleItem("prod_maggi_70", "Maggi 2-Minute Noodles 70g", 3.0, 14.0, 11.50, 42.0),
                        SaleItem("prod_lays_classic", "Lay's Classic Salted Chips 50g", 2.0, 20.0, 16.0, 40.0)
                    ),
                    paymentMethod = if (d % 2 == 0) PaymentMethod.UPI else PaymentMethod.CASH,
                    timestamp = now - (d * day) - (3 * hour)
                )
            )
            sales.add(
                createSale(
                    id = "sale_past_${d}_2",
                    items = listOf(
                        SaleItem("prod_amul_milk_500", "Amul Taaza Homogenised Toned Milk 500ml", 4.0, 28.0, 26.0, 112.0),
                        SaleItem("prod_parle_g", "Parle-G Glucose Biscuits 250g", 2.0, 30.0, 24.0, 60.0)
                    ),
                    paymentMethod = PaymentMethod.UPI,
                    timestamp = now - (d * day) - (7 * hour)
                )
            )
        }

        return sales
    }

    private fun createSale(
        id: String,
        items: List<SaleItem>,
        paymentMethod: PaymentMethod,
        customerId: String? = null,
        customerName: String = "",
        timestamp: Long
    ): SaleEntity {
        val itemsArray = JSONArray()
        var totalCost = 0.0
        var totalSelling = 0.0

        for (item in items) {
            val obj = JSONObject().apply {
                put("productId", item.productId)
                put("productName", item.productName)
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
                put("costPrice", item.costPrice)
                put("total", item.total)
            }
            itemsArray.put(obj)
            totalCost += (item.costPrice * item.quantity)
            totalSelling += item.total
        }

        return SaleEntity(
            id = id,
            itemsJson = itemsArray.toString(),
            itemCount = items.size,
            subtotal = totalSelling,
            discount = 0.0,
            total = totalSelling,
            estimatedCost = totalCost,
            estimatedGrossProfit = totalSelling - totalCost,
            paymentMethod = paymentMethod,
            customerId = customerId,
            customerName = customerName,
            createdAt = timestamp
        )
    }

    fun getDemoPurchases(): List<PurchaseEntity> {
        val now = System.currentTimeMillis()
        val day = 86400000L

        val purchases = mutableListOf<PurchaseEntity>()

        val p1Items = JSONArray().apply {
            put(JSONObject().apply {
                put("productId", "prod_maggi_70")
                put("productName", "Maggi 2-Minute Noodles 70g")
                put("quantity", 50.0)
                put("unitCost", 11.50)
                put("total", 575.0)
            })
            put(JSONObject().apply {
                put("productId", "prod_amul_milk_500")
                put("productName", "Amul Taaza Homogenised Toned Milk 500ml")
                put("quantity", 40.0)
                put("unitCost", 26.0)
                put("total", 1040.0)
            })
        }

        purchases.add(
            PurchaseEntity(
                id = "purch_1",
                supplierId = "sup_abc_traders",
                supplierName = "ABC Traders",
                invoiceNumber = "ABC/2026/089",
                itemsJson = p1Items.toString(),
                itemCount = 2,
                subtotal = 1615.0,
                discount = 50.0,
                tax = 80.75,
                total = 1645.75,
                paymentStatus = PaymentStatus.PAID,
                notes = "Regular weekly restock.",
                createdAt = now - (3 * day)
            )
        )

        val p2Items = JSONArray().apply {
            put(JSONObject().apply {
                put("productId", "prod_parle_g")
                put("productName", "Parle-G Glucose Biscuits 250g")
                put("quantity", 60.0)
                put("unitCost", 24.0)
                put("total", 1440.0)
            })
            put(JSONObject().apply {
                put("productId", "prod_tata_salt_1kg")
                put("productName", "Tata Salt Vacuum Evaporated 1kg")
                put("quantity", 50.0)
                put("unitCost", 24.0)
                put("total", 1200.0)
            })
        }

        purchases.add(
            PurchaseEntity(
                id = "purch_2",
                supplierId = "sup_balaji",
                supplierName = "Balaji Wholesale Agencies",
                invoiceNumber = "BAL-9921",
                itemsJson = p2Items.toString(),
                itemCount = 2,
                subtotal = 2640.0,
                discount = 0.0,
                tax = 132.0,
                total = 2772.0,
                paymentStatus = PaymentStatus.PARTIAL,
                notes = "₹1,500 paid on delivery, balance added to ledger.",
                createdAt = now - (7 * day)
            )
        )

        return purchases
    }

    fun getDemoStockMovements(): List<StockMovementEntity> {
        val now = System.currentTimeMillis()
        val day = 86400000L
        return listOf(
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                productId = "prod_maggi_70",
                productName = "Maggi 2-Minute Noodles 70g",
                type = StockMovementType.SALE,
                quantity = -4.0,
                previousStock = 12.0,
                newStock = 8.0,
                reason = "Sale #sale_today_1",
                createdAt = now - (2 * 3600000L)
            ),
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                productId = "prod_aashirvaad_atta_5kg",
                productName = "Aashirvaad Shudh Chakki Atta 5kg",
                type = StockMovementType.SALE,
                quantity = -1.0,
                previousStock = 5.0,
                newStock = 4.0,
                reason = "Sale #sale_today_2",
                createdAt = now - (4 * 3600000L)
            ),
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                productId = "prod_maggi_70",
                productName = "Maggi 2-Minute Noodles 70g",
                type = StockMovementType.PURCHASE,
                quantity = 50.0,
                previousStock = 2.0,
                newStock = 52.0,
                reason = "Invoice ABC/2026/089",
                createdAt = now - (3 * day)
            )
        )
    }

    fun getDemoPriceHistory(): List<PriceHistoryEntity> {
        val now = System.currentTimeMillis()
        val day = 86400000L
        return listOf(
            PriceHistoryEntity(
                id = UUID.randomUUID().toString(),
                productId = "prod_fortune_oil_1l",
                productName = "Fortune Sunlite Refined Sunflower Oil 1L",
                oldCostPrice = 135.0,
                newCostPrice = 142.0,
                oldSellingPrice = 165.0,
                newSellingPrice = 165.0,
                reason = "Supplier cost increased by ₹7/L. Margin reduced from 18.2% to 13.9%.",
                createdAt = now - (5 * day)
            ),
            PriceHistoryEntity(
                id = UUID.randomUUID().toString(),
                productId = "prod_maggi_70",
                productName = "Maggi 2-Minute Noodles 70g",
                oldCostPrice = 11.0,
                newCostPrice = 11.50,
                oldSellingPrice = 14.0,
                newSellingPrice = 14.0,
                reason = "Distributor price update.",
                createdAt = now - (14 * day)
            )
        )
    }

    fun getDemoInsights(): List<AIInsightEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            AIInsightEntity(
                id = "ins_maggi_low",
                type = InsightType.LOW_STOCK,
                severity = InsightSeverity.CRITICAL,
                title = "Maggi 70g Stock Critical",
                description = "Current stock is 8 packets. Based on average sales of 3.2 pkts/day, stock will run out in ~2.5 days.",
                reasoning = "Calculated from last 14 days of recorded sales velocity.",
                actionText = "Reorder 40 pkts",
                relatedProductIdsJson = "[\"prod_maggi_70\"]",
                createdAt = now - 3600000L
            ),
            AIInsightEntity(
                id = "ins_atta_reorder",
                type = InsightType.REORDER_ALERT,
                severity = InsightSeverity.WARNING,
                title = "Aashirvaad Atta 5kg below threshold",
                description = "Only 4 bags remaining (minimum threshold is 8). Suggested purchase: 15 bags from Balaji Agencies.",
                reasoning = "High velocity weekly staple.",
                actionText = "Add to Purchase",
                relatedProductIdsJson = "[\"prod_aashirvaad_atta_5kg\"]",
                createdAt = now - (2 * 3600000L)
            ),
            AIInsightEntity(
                id = "ins_dead_stock",
                type = InsightType.DEAD_STOCK,
                severity = InsightSeverity.WARNING,
                title = "₹1,125 locked in Slow Moving stock",
                description = "Chandan Agarbatti (25 boxes) has had 0 sales in the last 35 days.",
                reasoning = "No transaction logged since last month.",
                actionText = "Bundle with Puja staples",
                relatedProductIdsJson = "[\"prod_vintage_incense\"]",
                createdAt = now - (4 * 3600000L)
            ),
            AIInsightEntity(
                id = "ins_margin_warning",
                type = InsightType.MARGIN_DROP,
                severity = InsightSeverity.INFO,
                title = "Margin Warning on Fortune Sunflower Oil",
                description = "Cost increased from ₹135 to ₹142 while selling price remained ₹165. Margin dropped to 13.9%. Recommended selling price: ₹170.",
                reasoning = "Purchase invoice price update on 5 days ago.",
                actionText = "Update Selling Price to ₹170",
                relatedProductIdsJson = "[\"prod_fortune_oil_1l\"]",
                createdAt = now - (6 * 3600000L)
            )
        )
    }

    fun getDemoSettings(): ShopSettingsEntity {
        return ShopSettingsEntity(
            id = "default_shop",
            shopName = "Shree Balaji Kirana & Superstore",
            shopType = "Kirana / General Retail Store",
            ownerName = "Rajesh Kumar",
            phone = "+91 98765 00000",
            address = "Shop 12, Market Road, Sector 4, Indirapuram",
            currency = "₹",
            language = "Hinglish",
            defaultLowStockThreshold = 5.0,
            deadStockDaysThreshold = 30,
            setupCompleted = true,
            lastBackupTimestamp = System.currentTimeMillis(),
            autoGenerateInsights = true
        )
    }
}
