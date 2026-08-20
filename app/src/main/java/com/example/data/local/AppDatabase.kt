package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

class AppTypeConverters {
    @TypeConverter
    fun fromStockMovementType(value: StockMovementType): String = value.name

    @TypeConverter
    fun toStockMovementType(value: String): StockMovementType = try {
        StockMovementType.valueOf(value)
    } catch (e: Exception) {
        StockMovementType.ADJUSTMENT
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: Exception) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = try {
        PaymentStatus.valueOf(value)
    } catch (e: Exception) {
        PaymentStatus.PAID
    }

    @TypeConverter
    fun fromInsightType(value: InsightType): String = value.name

    @TypeConverter
    fun toInsightType(value: String): InsightType = try {
        InsightType.valueOf(value)
    } catch (e: Exception) {
        InsightType.GENERAL_BUSINESS
    }

    @TypeConverter
    fun fromInsightSeverity(value: InsightSeverity): String = value.name

    @TypeConverter
    fun toInsightSeverity(value: String): InsightSeverity = try {
        InsightSeverity.valueOf(value)
    } catch (e: Exception) {
        InsightSeverity.INFO
    }
}

@Database(
    entities = [
        ProductEntity::class,
        StockMovementEntity::class,
        SaleEntity::class,
        PurchaseEntity::class,
        CustomerEntity::class,
        CustomerLedgerEntryEntity::class,
        SupplierEntity::class,
        PriceHistoryEntity::class,
        AIInsightEntity::class,
        ShopSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun aiInsightDao(): AIInsightDao
    abstract fun shopSettingsDao(): ShopSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure all schema alterations for v2 are non-destructive
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shoppilot_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
