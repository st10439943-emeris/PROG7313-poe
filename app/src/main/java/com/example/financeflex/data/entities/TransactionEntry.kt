package com.example.financeflex.data.entities

/**
 * Data class representing a financial transaction entry in the FinanceFlex application.
 * Reconfigured from local Room storage to sync seamlessly with Firebase Firestore online.
 */
data class TransactionEntry(
    // Firestore uses unique text strings for IDs instead of auto incrementing numbers
    var documentId: String = "",

    val userId: String = "",
    val categoryId: Int = 0,
    val categoryName: String = "",
    val entryType: String = "", // "Expense" or "Income"
    val amount: Double = 0.0,
    val entryDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val description: String = "",

    // Changed from receiptImagePath to receiptUrl to store the cloud storage link
    val receiptUrl: String? = null
)