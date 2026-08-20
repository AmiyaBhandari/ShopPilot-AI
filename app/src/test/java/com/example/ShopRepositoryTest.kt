package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.InsufficientStockException
import com.example.data.repository.ShopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShopRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ShopRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ShopRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `save product and record sale with atomic stock deduction and profit calculation`() {
        runBlocking {
            val product = ProductEntity(
                id = "prod-1",
                name = "Aashirvaad Atta 5kg",
                costPrice = 210.0,
                sellingPrice = 260.0,
                currentStock = 20.0
            )
            repository.saveProduct(product)

            val saleItem = SaleItem(
                productId = "prod-1",
                productName = "Aashirvaad Atta 5kg",
                quantity = 3.0,
                unitPrice = 260.0,
                costPrice = 210.0,
                total = 780.0
            )

            val (sale, undoAction) = repository.recordSale(
                items = listOf(saleItem),
                paymentMethod = PaymentMethod.CASH
            )

            assertEquals(780.0, sale.total, 0.01)
            assertEquals(150.0, sale.estimatedGrossProfit, 0.01) // (260 - 210) * 3 = 150

            val updatedProduct = repository.getProductById("prod-1")
            assertNotNull(updatedProduct)
            assertEquals(17.0, updatedProduct!!.currentStock, 0.01)

            // Test Undo
            assertNotNull(undoAction)
            val undoSuccess = repository.undoAction(undoAction!!)
            assertTrue(undoSuccess)

            val restoredProduct = repository.getProductById("prod-1")
            assertEquals(20.0, restoredProduct!!.currentStock, 0.01)
        }
    }

    @Test(expected = InsufficientStockException::class)
    fun `record sale with insufficient stock throws InsufficientStockException`() {
        runBlocking {
            val product = ProductEntity(
                id = "prod-2",
                name = "Sugar 1kg",
                costPrice = 38.0,
                sellingPrice = 44.0,
                currentStock = 2.0
            )
            repository.saveProduct(product)

            val saleItem = SaleItem(
                productId = "prod-2",
                productName = "Sugar 1kg",
                quantity = 5.0, // Asking for 5 when only 2 in stock
                unitPrice = 44.0,
                costPrice = 38.0,
                total = 220.0
            )

            repository.recordSale(
                items = listOf(saleItem),
                paymentMethod = PaymentMethod.CASH
            )
        }
    }

    @Test
    fun `record purchase updates stock and weighted average cost price`() {
        runBlocking {
            val product = ProductEntity(
                id = "prod-3",
                name = "Basmati Rice 1kg",
                costPrice = 90.0,
                sellingPrice = 120.0,
                currentStock = 10.0
            )
            repository.saveProduct(product)

            val purchaseItem = PurchaseItem(
                productId = "prod-3",
                productName = "Basmati Rice 1kg",
                quantity = 10.0,
                unitCost = 110.0,
                total = 1100.0
            )

            val (purchase, undoAction) = repository.recordPurchase(
                supplierId = null,
                supplierName = "Wholesaler India",
                invoiceNumber = "INV-101",
                items = listOf(purchaseItem)
            )

            assertEquals(1100.0, purchase.total, 0.01)

            val updatedProduct = repository.getProductById("prod-3")
            assertNotNull(updatedProduct)
            assertEquals(20.0, updatedProduct!!.currentStock, 0.01)
            // Updated to latest purchase invoice cost
            assertEquals(110.0, updatedProduct.costPrice, 0.01)

            // Undo purchase
            assertNotNull(undoAction)
            val undoSuccess = repository.undoAction(undoAction!!)
            assertTrue(undoSuccess)

            val revertedProduct = repository.getProductById("prod-3")
            assertEquals(10.0, revertedProduct!!.currentStock, 0.01)
        }
    }

    @Test
    fun `customer payment updates khata balance and creates ledger entry atomically`() {
        runBlocking {
            val customer = CustomerEntity(
                id = "cust-1",
                name = "Ramesh Kumar",
                phone = "9876543210",
                creditBalance = 500.0
            )
            repository.addCustomer(customer)

            val undoAction = repository.recordCustomerPayment("cust-1", 200.0, "Partial cash payment")

            val updatedCustomer = db.customerDao().getCustomerById("cust-1")
            assertNotNull(updatedCustomer)
            assertEquals(300.0, updatedCustomer!!.creditBalance, 0.01)

            val ledgers = db.customerDao().getAllLedgerEntries().first()
            assertTrue(ledgers.any { it.customerId == "cust-1" && it.amount == 200.0 })

            // Undo payment
            assertNotNull(undoAction)
            val undoSuccess = repository.undoAction(undoAction!!)
            assertTrue(undoSuccess)

            val revertedCustomer = db.customerDao().getCustomerById("cust-1")
            assertEquals(500.0, revertedCustomer!!.creditBalance, 0.01)
        }
    }

    @Test
    fun `backup export and import correctly restores full snapshot`() {
        runBlocking {
            val product = ProductEntity(
                id = "prod-bk",
                name = "Colgate Dental Cream 100g",
                costPrice = 45.0,
                sellingPrice = 58.0,
                currentStock = 15.0
            )
            repository.saveProduct(product)

            val json = repository.exportShopDataJson()
            assertNotNull(json)
            assertTrue(json.contains("Colgate Dental Cream 100g"))

            val preview = repository.parseBackupPreview(json)
            assertNotNull(preview)
            assertEquals(1, preview!!.productCount)

            // Clear and restore
            repository.clearAllData()
            val emptyProduct = repository.getProductById("prod-bk")
            assertNull(emptyProduct)

            val restoreSuccess = repository.importShopDataJson(json)
            assertTrue(restoreSuccess)

            val restoredProduct = repository.getProductById("prod-bk")
            assertNotNull(restoredProduct)
            assertEquals("Colgate Dental Cream 100g", restoredProduct!!.name)
        }
    }
}
