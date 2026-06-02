package com.example.financeflex.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.financeflex.data.entities.TransactionEntry

//DAO for managing TransactionEntry records in the Room database.
//source: Android Developers (2024). Room Persistence Library Guide.

@Dao
interface TransactionDao {
    
//Inserts a new transaction entry into the database.
    
    @Insert
    suspend fun insertEntry(entry: TransactionEntry)
    
//Retrieves all transaction entries for a specific user ordered by newest date and time first.

    @Query("""
        SELECT * FROM transaction_entries 
        WHERE userId = :userId 
        ORDER BY entryDate DESC, startTime DESC
    """)
    suspend fun getEntriesByUser(userId: Int): List<TransactionEntry>

//Retrieves transaction entries for a user within a specific date range.
    
    @Query("""
        SELECT * FROM transaction_entries 
        WHERE userId = :userId 
        AND entryDate BETWEEN :startDate AND :endDate
        ORDER BY entryDate DESC, startTime DESC
    """)
    suspend fun getEntriesForPeriod(
        userId: Int,
        startDate: String,
        endDate: String
    ): List<TransactionEntry>
    
//Calculates total amount for a specific entry type (income/expense) within a given date range.
    
    @Query("""
        SELECT SUM(amount) FROM transaction_entries
        WHERE userId = :userId
        AND entryType = :entryType
        AND entryDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalForType(
        userId: Int,
        entryType: String,
        startDate: String,
        endDate: String
    ): Double?
}
