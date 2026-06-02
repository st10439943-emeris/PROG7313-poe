package com.example.financeflex.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financeflex.data.entities.Category
/*
 * CategoryDao defines database operations for the Category entity.
 * Source: Android Developers (2024) – Room Persistence Library
 */
@Dao
interface CategoryDao {
    
//Inserts a single category into the database.
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)
    
//Inserts a list of categories.

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Category>)
    
//Retrieves all categories for a specific user.

    @Query("""
        SELECT * FROM categories 
        WHERE userId = :userId 
        GROUP BY categoryName, categoryType
        ORDER BY categoryType, categoryName
    """)
    suspend fun getCategoriesByUser(userId: Int): List<Category>

    // Retrieves categories filtered by type (e.g., income or expense).

    @Query("""
        SELECT * FROM categories 
        WHERE userId = :userId 
        AND categoryType = :categoryType 
        GROUP BY categoryName, categoryType
        ORDER BY categoryName
    """)
    suspend fun getCategoriesByType(userId: Int, categoryType: String): List<Category>

    //Retrieves a specific category based on name and type.
    
    @Query("""
        SELECT * FROM categories 
        WHERE userId = :userId 
        AND categoryName = :categoryName 
        AND categoryType = :categoryType
        LIMIT 1
    """)
    suspend fun getCategoryByNameAndType(
        userId: Int,
        categoryName: String,
        categoryType: String
    ): Category?
//Deletes a custom category created by the user.
    @Query("DELETE FROM categories WHERE categoryId = :categoryId AND isDefault = 0")
    suspend fun deleteCustomCategory(categoryId: Int)
}
