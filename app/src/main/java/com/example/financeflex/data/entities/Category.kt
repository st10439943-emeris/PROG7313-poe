package com.example.financeflex.data.entities

/**
 * Data class representing a Category in the FinanceFlex application.
 * Reconfigured from local Room storage to sync seamlessly with Firebase Firestore online.
 */
data class Category(
    // Firestore uses unique text strings for document IDs
    var documentId: String = "",

    val categoryId: Int = 0, // Keeping this as an Int so we don't break your dropdown menus!
    val userId: String = "",
    val categoryName: String = "",
    val categoryType: String = "", // "Expense" or "Income"
    val iconName: String = "",
    val isDefault: Boolean = false
)