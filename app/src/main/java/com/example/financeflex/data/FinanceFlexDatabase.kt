package com.example.financeflex.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.financeflex.data.dao.BudgetGoalDao
import com.example.financeflex.data.dao.CategoryDao
import com.example.financeflex.data.dao.TransactionDao
import com.example.financeflex.data.dao.UserDao
import com.example.financeflex.data.entities.BudgetGoal
import com.example.financeflex.data.entities.Category
import com.example.financeflex.data.entities.TransactionEntry
import com.example.financeflex.data.entities.User

/**
 * Main Room Database class for the FinanceFlex application.
 * This class defines the database configuration and serves as the central
 * access point for all DAO interfaces in the application.
 * The database version is set to support schema migrations.
 * Reference: Android Developers (2024). Room Database Guide
 */

@Database(
    entities = [
        User::class,
        Category::class,
        BudgetGoal::class,
        TransactionEntry::class
    ],
    version = 5,
    exportSchema = false
)
abstract class FinanceFlexDatabase : RoomDatabase() {
    
//Provides access to User-related, Category-related, BudgetGoal-related, TransactionEntry-related database operations.
    
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        
//Volatile ensures visibility of INSTANCE across all threads.
        
        @Volatile
        private var INSTANCE: FinanceFlexDatabase? = null
        
//Returns a singleton instance of the database, if the database does not exist, it is created using Room's databaseBuilder.
        
        fun getDatabase(context: Context): FinanceFlexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceFlexDatabase::class.java,
                    "finance_flex_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
