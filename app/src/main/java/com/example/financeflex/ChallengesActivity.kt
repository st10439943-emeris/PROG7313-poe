package com.example.financeflex

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflex.data.entities.TransactionEntry
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

// You track user progress for the Emergency Fund Plan and Frugal February in this file.
class ChallengesActivity : AppCompatActivity() {

    private val tag = "FinanceFlexWorkout"
    private var userId: String = "-1"

    // You link your screen elements here.
    private lateinit var tvEmergencyFundProgress: TextView
    private lateinit var tvFrugalFebProgress: TextView
    private lateinit var tvChallengesMessage: TextView

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenges)

        userId = intent.getStringExtra("userId") ?: "-1"

        tvEmergencyFundProgress = findViewById(R.id.tvEmergencyFundProgress)
        tvFrugalFebProgress = findViewById(R.id.tvFrugalFebProgress)
        tvChallengesMessage = findViewById(R.id.tvChallengesMessage)

        loadWorkoutPlans()

        // You configure the bottom navigation bar.
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        setupBottomNavigation(bottomNavigation)
    }

    private fun loadWorkoutPlans() {
        if (userId == "-1") {
            tvChallengesMessage.setTextColor(getColor(R.color.finance_red))
            tvChallengesMessage.text = "User not found. Please login again."
            return
        }

        val calendar = Calendar.getInstance()
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

        FirebaseManager.firestore.collection("transactions")
            .whereEqualTo("userId", userId.toString())
            .get()
            .addOnSuccessListener { documents ->
                val allEntries = documents.toObjects(TransactionEntry::class.java)

                // You filter transactions matching the current month.
                val currentMonthEntries = allEntries.filter { entry ->
                    entry.entryDate.startsWith(currentMonthStr)
                }

                calculateEmergencyFund(currentMonthEntries)
                calculateFrugalPlan(currentMonthEntries)

                // You clear the loading message.
                tvChallengesMessage.text = ""
            }
            .addOnFailureListener { exception ->
                tvChallengesMessage.setTextColor(getColor(R.color.finance_red))
                tvChallengesMessage.text = "Could not load workout plans."
                Log.e(tag, "Error loading transactions", exception)
            }
    }

    // You calculate progress for the R200 Emergency Fund Plan.
    private fun calculateEmergencyFund(entries: List<TransactionEntry>) {
        val emergencyGoal = 200.0
        // You check for income labeled Savings or Emergency.
        val savedAmount = entries
            .filter { entry -> entry.entryType == "Income" && (entry.categoryName.contains("Sav") || entry.categoryName.contains("Emerg")) }
            .sumOf { entry -> entry.amount }

        tvEmergencyFundProgress.text = "${currencyFormatter.format(savedAmount)} / ${currencyFormatter.format(emergencyGoal)} Goal"
    }

    // You calculate progress for the R500 Frugal February Plan.
    private fun calculateFrugalPlan(entries: List<TransactionEntry>) {
        val frugalLimit = 500.0
        // You sum total expenses for the month.
        val totalSpent = entries
            .filter { entry -> entry.entryType == "Expense" }
            .sumOf { entry -> entry.amount }

        tvFrugalFebProgress.text = "${currencyFormatter.format(totalSpent)} / ${currencyFormatter.format(frugalLimit)} Limit"
    }

    // You configure the bottom navigation bar to match your app structure.
    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        // You remove highlighting from all icons.
        bottomNavigation.menu.setGroupCheckable(0, false, true)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    val targetIntent = Intent(this, DashboardActivity::class.java)
                    targetIntent.putExtra("userId", userId)
                    startActivity(targetIntent)
                    finish()
                    true
                }
                R.id.nav_entries -> {
                    val targetIntent = Intent(this, LogEntryActivity::class.java)
                    targetIntent.putExtra("userId", userId)
                    startActivity(targetIntent)
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    val targetIntent = Intent(this, CategoryActivity::class.java)
                    targetIntent.putExtra("userId", userId)
                    startActivity(targetIntent)
                    finish()
                    true
                }
                R.id.nav_reports -> {
                    val targetIntent = Intent(this, ViewEntriesActivity::class.java)
                    targetIntent.putExtra("userId", userId)
                    startActivity(targetIntent)
                    finish()
                    true
                }
                R.id.nav_goals -> {
                    val targetIntent = Intent(this, BudgetGoalActivity::class.java)
                    targetIntent.putExtra("userId", userId)
                    startActivity(targetIntent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}