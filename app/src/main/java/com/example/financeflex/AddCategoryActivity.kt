package com.example.financeflex

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity responsible for creating and saving new financial categories.
 * Data is stored directly in Firebase Firestore.
 */
class AddCategoryActivity : AppCompatActivity() {

    private val tag = "FinanceFlexAddCategory"
    private var userId: String = "-1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        userId = intent.getStringExtra("userId") ?: "-1"

        val edtCategoryName = findViewById<EditText>(R.id.edtCategoryName)
        val rbExpense = findViewById<RadioButton>(R.id.rbExpense)
        val edtIconName = findViewById<EditText>(R.id.edtIconName)
        val btnSaveCategory = findViewById<Button>(R.id.btnSaveCategory)
        val tvAddCategoryMessage = findViewById<TextView>(R.id.tvAddCategoryMessage)

        btnSaveCategory.setOnClickListener {
            val categoryName = edtCategoryName.text.toString().trim()
            val iconName = edtIconName.text.toString().trim()
            val categoryType = if (rbExpense.isChecked) "Expense" else "Income"

            if (userId == "-1") {
                tvAddCategoryMessage.setTextColor(getColor(R.color.finance_red))
                tvAddCategoryMessage.text = "User not found. Please login again."
                return@setOnClickListener
            }

            if (categoryName.isEmpty()) {
                edtCategoryName.error = "Category name is required"
                return@setOnClickListener
            }

            // Check for duplicates in Firebase before saving
            FirebaseManager.firestore.collection("categories")
                .whereEqualTo("userId", userId.toString())
                .whereEqualTo("categoryName", categoryName)
                .whereEqualTo("categoryType", categoryType)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        tvAddCategoryMessage.setTextColor(getColor(R.color.finance_red))
                        tvAddCategoryMessage.text = "Category already exists"
                        return@addOnSuccessListener
                    }

                    // Create category map for Firebase
                    val categoryData = hashMapOf(
                        "userId" to userId.toString(),
                        "categoryName" to categoryName,
                        "categoryType" to categoryType,
                        "iconName" to iconName,
                        "isDefault" to false
                    )

                    // Save to Firebase
                    FirebaseManager.firestore.collection("categories")
                        .add(categoryData)
                        .addOnSuccessListener {
                            tvAddCategoryMessage.setTextColor(getColor(R.color.finance_green))
                            tvAddCategoryMessage.text = "Category saved successfully"
                            Log.d(tag, "Category saved: $categoryName")

                            edtCategoryName.text.clear()
                            edtIconName.text.clear()
                        }
                        .addOnFailureListener { e ->
                            tvAddCategoryMessage.setTextColor(getColor(R.color.finance_red))
                            tvAddCategoryMessage.text = "Could not save category"
                            Log.e(tag, "Error saving category", e)
                        }
                }
                .addOnFailureListener { e ->
                    tvAddCategoryMessage.setTextColor(getColor(R.color.finance_red))
                    tvAddCategoryMessage.text = "Could not check categories"
                    Log.e(tag, "Error checking duplicates", e)
                }
        }
    }
}