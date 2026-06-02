package com.example.financeflex

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflex.data.entities.Category
import com.example.financeflex.data.entities.TransactionEntry
import com.example.financeflex.data.models.CategoryTotal
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Activity allows users to view, filter, and analyse financial transaction entries.
 * Data is retrieved directly from Firebase Firestore.
 */
class ViewEntriesActivity : AppCompatActivity() {

    private val tag = "FinanceFlexViewEntries"
    private var userId: String = "-1"

    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var spinnerFilterCategory: Spinner
    private lateinit var tvEntriesMessage: TextView
    private lateinit var tvEntriesList: TextView
    private lateinit var tvReportSummary: TextView
    private lateinit var pieChartView: PieChartView

    private var selectedStartDate = ""
    private var selectedEndDate = ""
    private var categoryList: List<Category> = emptyList()

    private val currencyFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_entries)

        userId = intent.getStringExtra("userId") ?: "-1"

        btnStartDate = findViewById(R.id.btnStartDate)
        btnEndDate = findViewById(R.id.btnEndDate)
        spinnerFilterCategory = findViewById(R.id.spinnerFilterCategory)

        val btnLoadEntries = findViewById<Button>(R.id.btnLoadEntries)
        val btnViewReceipts = findViewById<Button>(R.id.btnViewReceipts)

        tvEntriesMessage = findViewById(R.id.tvEntriesMessage)
        tvEntriesList = findViewById(R.id.tvEntriesList)
        tvReportSummary = findViewById(R.id.tvReportSummary)
        pieChartView = findViewById(R.id.pieChartView)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        setDefaultDates()
        loadCategoryFilter()

        btnStartDate.setOnClickListener { showDatePicker(true) }
        btnEndDate.setOnClickListener { showDatePicker(false) }
        btnLoadEntries.setOnClickListener { loadEntries() }

        btnViewReceipts.setOnClickListener {
            val intent = Intent(this, ReceiptViewerActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        setupBottomNavigation(bottomNavigation)
    }

    private fun setDefaultDates() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedEndDate = dateFormat.format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        selectedStartDate = dateFormat.format(calendar.time)

        btnStartDate.text = "From\n$selectedStartDate"
        btnEndDate.text = "To\n$selectedEndDate"
    }

    private fun loadCategoryFilter() {
        if (userId == "-1") return

        FirebaseManager.firestore.collection("categories")
            .whereEqualTo("userId", userId.toString())
            .get()
            .addOnSuccessListener { documents ->
                categoryList = documents.toObjects(Category::class.java)
                val filterItems = mutableListOf("All Categories")
                filterItems.addAll(categoryList.map { "${it.categoryType}: ${it.categoryName}" })

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterItems)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFilterCategory.adapter = adapter
            }
            .addOnFailureListener { e ->
                tvEntriesMessage.text = "Error loading filters."
                Log.e(tag, "Error", e)
            }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val date = String.format("%04d-%02d-%02d", y, m + 1, d)
            if (isStartDate) {
                selectedStartDate = date
                btnStartDate.text = "From\n$selectedStartDate"
            } else {
                selectedEndDate = date
                btnEndDate.text = "To\n$selectedEndDate"
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadEntries() {
        if (userId == "-1") return

        FirebaseManager.firestore.collection("transactions")
            .whereEqualTo("userId", userId.toString())
            .get()
            .addOnSuccessListener { documents ->
                val allEntries = documents.toObjects(TransactionEntry::class.java)
                var entries = allEntries.filter { it.entryDate in selectedStartDate..selectedEndDate }

                val pos = spinnerFilterCategory.selectedItemPosition
                if (pos > 0) {
                    val cat = categoryList[pos - 1]
                    entries = entries.filter { it.categoryId == cat.categoryId }
                }

                tvEntriesList.text = formatEntries(entries)
                tvReportSummary.text = buildReportSummary(entries)
                pieChartView.setData(buildExpenseCategoryTotals(entries))
            }
    }

    private fun formatEntries(entries: List<TransactionEntry>): String {
        return entries.joinToString(separator = "\n\n") { entry ->
            val status = if (entry.receiptUrl.isNullOrEmpty()) "No receipt" else "Receipt attached"
            "${entry.entryType}: ${entry.categoryName}\nAmount: ${currencyFormatter.format(entry.amount)}\nDate: ${entry.entryDate}\nNote: ${entry.description}\n$status"
        }
    }

    private fun buildReportSummary(entries: List<TransactionEntry>): String {
        val income = entries.filter { it.entryType == "Income" }.sumOf { it.amount }
        val expenses = entries.filter { it.entryType == "Expense" }.sumOf { it.amount }
        return "Total income: ${currencyFormatter.format(income)}\nTotal expenses: ${currencyFormatter.format(expenses)}\nBalance: ${currencyFormatter.format(income - expenses)}"
    }

    private fun buildExpenseCategoryTotals(entries: List<TransactionEntry>): List<CategoryTotal> {
        return entries.filter { it.entryType == "Expense" }
            .groupBy { it.categoryName }
            .map { CategoryTotal(it.key, it.value.sumOf { entry -> entry.amount }) }
            .sortedByDescending { it.totalAmount }
    }

    private fun setupBottomNavigation(nav: BottomNavigationView) {
        nav.selectedItemId = R.id.nav_reports
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { finish(); true }
                R.id.nav_entries -> { startActivity(Intent(this, LogEntryActivity::class.java).putExtra("userId", userId)); true }
                R.id.nav_categories -> { startActivity(Intent(this, CategoryActivity::class.java).putExtra("userId", userId)); true }
                R.id.nav_reports -> true
                R.id.nav_goals -> { startActivity(Intent(this, BudgetGoalActivity::class.java).putExtra("userId", userId)); true }
                else -> false
            }
        }
    }
}