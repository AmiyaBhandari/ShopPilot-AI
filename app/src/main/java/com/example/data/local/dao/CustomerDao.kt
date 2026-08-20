package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.CustomerLedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE creditBalance > 0 ORDER BY creditBalance DESC")
    fun getCustomersWithCredit(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET creditBalance = creditBalance + :delta, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCreditBalance(id: String, delta: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customers WHERE name = :name LIMIT 1")
    suspend fun getCustomerByName(name: String): CustomerEntity?

    @Query("SELECT * FROM customer_ledger_entries ORDER BY createdAt DESC")
    fun getAllLedgerEntries(): Flow<List<CustomerLedgerEntryEntity>>

    @Query("SELECT * FROM customer_ledger_entries WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getLedgerForCustomer(customerId: String): Flow<List<CustomerLedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: CustomerLedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<CustomerLedgerEntryEntity>)

    @Query("DELETE FROM customer_ledger_entries WHERE referenceId = :refId")
    suspend fun deleteLedgerEntriesByReferenceId(refId: String)

    @Query("DELETE FROM customer_ledger_entries WHERE id = :id")
    suspend fun deleteLedgerEntryById(id: String)

    @Query("DELETE FROM customer_ledger_entries")
    suspend fun deleteAllLedgerEntries()

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
