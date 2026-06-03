package com.example.financeflex

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflex.data.entities.Category
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Activity responsible for displaying and managing user categories.
 * Retrieves categories from Firebase Firestore.
 * Automatically inserts default categories for new users.
 */
class CategoryActivity : AppCompatActivity() {

    private val tag = "FinanceFlexCategories"
    private var userId: String = "-1"

    private lateinit var tvCategoriesList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        userId = intent.getStringExtra("userId") ?: "-1"

        val btnAddCategory = findViewById<Button>(R.id.btnAddCategory)
        tvCategoriesList = findViewById(R.id.tvCategoriesList)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.selectedItemId = R.id.nav_categories

        btnAddCategory.setOnClickListener {
            val intent = Intent(this, AddCategoryActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    finish()
                    true
                }
                R.id.nav_categories -> true
                R.id.nav_entries -> {
                    val intent = Intent(this, LogEntryActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    true
                }
                R.id.nav_reports -> {
                    val intent = Intent(this, ViewEntriesActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    true
                }
                R.id.nav_goals -> {
                    val intent = Intent(this, BudgetGoalActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadCategories() {
        if (userId == "-1") {
            tvCategoriesList.text = "User not found. Please login again."
            return
        }

        tvCategoriesList.text = "Loading cloud categories..."

        FirebaseManager.firestore.collection("categories")
            .whereEqualTo("userId", userId.toString())
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    insertDefaultCategories()
                } else {
                    val categories = documents.toObjects(Category::class.java)
                    val displayText = categories.joinToString(separator = "\n\n") { category ->
                        "${category.categoryType}: ${category.categoryName}"
                    }
                    tvCategoriesList.text = displayText
                    Log.d(tag, "Loaded ${categories.size} cloud categories")
                }
            }
            .addOnFailureListener { exception ->
                tvCategoriesList.text = "Could not load categories."
                Log.e(tag, "Error loading categories", exception)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun insertDefaultCategories() {
        tvCategoriesList.text = "Setting up default categories..."

        val defaultCategories = listOf(
            Category(categoryId = 1, userId = userId.toString(), categoryName = "Food", categoryType = "Expense", iconName = "food", isDefault = true),
            Category(categoryId = 2, userId = userId.toString(), categoryName = "Transport", categoryType = "Expense", iconName = "car", isDefault = true),
            Category(categoryId = 3, userId = userId.toString(), categoryName = "Entertainment", categoryType = "Expense", iconName = "fun", isDefault = true),
            Category(categoryId = 4, userId = userId.toString(), categoryName = "Bills", categoryType = "Expense", iconName = "bill", isDefault = true),
            Category(categoryId = 5, userId = userId.toString(), categoryName = "Health", categoryType = "Expense", iconName = "health", isDefault = true),
            Category(categoryId = 6, userId = userId.toString(), categoryName = "Salary", categoryType = "Income", iconName = "salary", isDefault = true),
            Category(categoryId = 7, userId = userId.toString(), categoryName = "Allowance", categoryType = "Income", iconName = "allowance", isDefault = true)
        )

        val batch = FirebaseManager.firestore.batch()
        val collectionRef = FirebaseManager.firestore.collection("categories")

        defaultCategories.forEach { category ->
            val docRef = collectionRef.document()
            category.documentId = docRef.id
            batch.set(docRef, category)
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(tag, "Default categories successfully added to Firestore")
                loadCategories()
            }
            .addOnFailureListener { e ->
                tvCategoriesList.text = "Failed to set up defaults."
                Log.e(tag, "Error inserting defaults", e)
            }
    }
}