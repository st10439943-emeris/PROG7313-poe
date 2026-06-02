package com.example.financeflex.data.entities

/**
 * Data class representing a Budget Goal in the FinanceFlex application.
 * Reconfigured from local Room storage to sync seamlessly with Firebase Firestore online.
 */
data class BudgetGoal(
    // Firestore uses unique text strings for document IDs instead of auto-incrementing numbers
    var documentId: String = "",

    val userId: String = "", // Changed to String to match our Firestore user structure
    val month: Int = 0,
    val year: Int = 0,
    val minGoal: Double = 0.0,
    val maxGoal: Double = 0.0
)