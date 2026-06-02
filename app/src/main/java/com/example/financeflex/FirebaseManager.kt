package com.example.financeflex

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * FirebaseManager provides global online database and cloud storage instances
 * to handle cloud synchronization for the FinanceFlex application.
 */
object FirebaseManager {

    // Online Database (Firestore) instance
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Cloud Storage (Receipt Images) instance
    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
}