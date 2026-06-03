package com.example.financeflex

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflex.data.entities.BudgetGoal
import com.example.financeflex.data.entities.TransactionEntry
import com.example.financeflex.data.models.CategoryTotal
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.RequiresApi

/**
 * Main dashboard screen for the FinanceFlex application.
 * This activity provides a real-time financial overview for the user,
 * including income, expenses, budget status, goal progress, financial fitness score,
 * and personalized financial tips entirely from Firebase Firestore.
 */
class DashboardActivity : AppCompatActivity() {

    private val tag = "FinanceFlexDashboard"
    private var userId: String = "-1"

    private lateinit var tvWelcomeMessage: TextView
    private lateinit var tvSpentAmount: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvBudgetStatus: TextView
    private lateinit var tvGoalProgressText: TextView
    private lateinit var progressGoalZone: ProgressBar
    private lateinit var tvMoneyMoves: TextView
    private lateinit var tvFinancialFitness: TextView
    private lateinit var tvFinancialFitnessStatus: TextView
    private lateinit var tvMoneyTip: TextView
    private lateinit var dashboardPieChart: PieChartView

    private val currencyFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        //Bind UI elements
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage)
        tvSpentAmount = findViewById(R.id.tvSpentAmount)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus)
        tvGoalProgressText = findViewById(R.id.tvGoalProgressText)
        progressGoalZone = findViewById(R.id.progressGoalZone)
        tvMoneyMoves = findViewById(R.id.tvMoneyMoves)
        tvFinancialFitness = findViewById(R.id.tvFinancialFitness)
        tvFinancialFitnessStatus = findViewById(R.id.tvFinancialFitnessStatus)
        tvMoneyTip = findViewById(R.id.tvMoneyTip)
        dashboardPieChart = findViewById(R.id.dashboardPieChart)

        val tvHamburgerMenu = findViewById<TextView>(R.id.tvHamburgerMenu)

        tvHamburgerMenu.setOnClickListener {
            showDashboardMenu(it)
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        val username = intent.getStringExtra("username") ?: "user"
        userId = intent.getStringExtra("userId") ?: "-1"

        tvWelcomeMessage.text = "Welcome back, $username"

        // Load dashboard analytics
        loadDashboardData()
        setupBottomNavigation(bottomNavigation)

        Log.d(tag, "Dashboard opened successfully for username: $username")
        tvFinancialFitness.setOnClickListener { view ->
            val targetIntent = Intent(this, ChallengesActivity::class.java)
            targetIntent.putExtra("userId", userId)
            startActivity(targetIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun showDashboardMenu(anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView)

        popupMenu.menu.add("Account Settings")
        popupMenu.menu.add("Logout")

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title.toString()) {
                "Account Settings" -> {
                    val intent = Intent(this, AccountSettingsActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    true
                }

                "Logout" -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    /**
     * Loads and processes all financial data for the dashboard view.
     * Both Goals and Transactions are now pulled directly from Firebase Firestore!
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDashboardData() {
        if (userId == "-1") {
            tvSpentAmount.text = "R0.00"
            tvTotalIncome.text = "Income Fuel: R0.00"
            tvBudgetStatus.text = "Goal Zone: User not found"
            tvGoalProgressText.text = "No goal progress available."
            progressGoalZone.progress = 0
            tvMoneyMoves.text = "Please login again to track your money moves."
            tvFinancialFitness.text = "Financial Fitness Score: 0/100"
            tvFinancialFitnessStatus.text = "Form: User not found"
            tvMoneyTip.text = "Please login again to view your coach’s tip."
            dashboardPieChart.setData(emptyList())
            return
        }

        val startDate = getCurrentMonthStartDate()
        val endDate = getCurrentMonthEndDate()
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        tvBudgetStatus.text = "Goal Zone: Loading cloud data..."

        // 1. Fetch the Budget Goal from Firestore!
        FirebaseManager.firestore.collection("budgetGoals")
            .whereEqualTo("userId", userId.toString())
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .get()
            .addOnSuccessListener { goalDocs ->

                val budgetGoal = if (!goalDocs.isEmpty) {
                    goalDocs.documents[0].toObject(BudgetGoal::class.java)
                } else null

                // 2. Fetch the Transactions directly from Firebase Firestore!
                FirebaseManager.firestore.collection("transactions")
                    .whereEqualTo("userId", userId.toString())
                    .get()
                    .addOnSuccessListener { transDocs ->
                        // Convert cloud data back to Kotlin objects
                        val allEntries = transDocs.toObjects(TransactionEntry::class.java)

                        // Filter for the current month locally
                        val entries = allEntries.filter { it.entryDate in startDate..endDate }

                        // Do all the math
                        val totalExpenses = entries.filter { it.entryType == "Expense" }.sumOf { it.amount }
                        val totalIncome = entries.filter { it.entryType == "Income" }.sumOf { it.amount }

                        // Update the screen
                        tvSpentAmount.text = currencyFormatter.format(totalExpenses)
                        tvTotalIncome.text = "Income Fuel: ${currencyFormatter.format(totalIncome)}"

                        updateGoalZone(totalExpenses, budgetGoal?.minGoal, budgetGoal?.maxGoal)
                        updateMoneyMoves(entries)
                        updateFinancialFitnessScore(entries, totalIncome, totalExpenses, budgetGoal?.minGoal, budgetGoal?.maxGoal)
                        updateSmartTip(entries, totalIncome, totalExpenses, budgetGoal?.maxGoal)

                        updateMonthlyChallenges(entries)

                        val expenseCategoryTotals = buildExpenseCategoryTotals(entries)
                        dashboardPieChart.setData(expenseCategoryTotals)

                        Log.d(tag, "Dashboard data loaded from Firestore")
                    }
                    .addOnFailureListener { exception ->
                        tvBudgetStatus.text = "Goal Zone: Cloud load failed"
                        tvGoalProgressText.text = "Could not load transactions."
                        Log.e(tag, "Error loading cloud transactions", exception)
                    }

            }
            .addOnFailureListener { exception ->
                tvBudgetStatus.text = "Goal Zone: Goal load failed"
                Log.e(tag, "Error loading cloud budget goals", exception)
            }
    }

    /**
     * Calculates and displays budget goal progress.
     */
    private fun updateGoalZone(
        totalExpenses: Double,
        minGoal: Double?,
        maxGoal: Double?
    ) {
        if (minGoal == null || maxGoal == null || maxGoal <= 0.0) {
            tvBudgetStatus.text = "Goal Zone: Set your monthly goals"
            tvBudgetStatus.setTextColor(getColor(R.color.finance_teal))
            tvGoalProgressText.text = "Set a maximum goal to activate your Goal Zone."
            progressGoalZone.progress = 0
            return
        }

        val progress = ((totalExpenses / maxGoal) * 100).toInt().coerceIn(0, 100)
        progressGoalZone.progress = progress

        tvGoalProgressText.text =
            "${currencyFormatter.format(totalExpenses)} of ${currencyFormatter.format(maxGoal)} max goal used."

        when {
            totalExpenses < minGoal -> {
                tvBudgetStatus.text =
                    "Goal Zone: Below training range (${currencyFormatter.format(totalExpenses)} / ${currencyFormatter.format(minGoal)})"
                tvBudgetStatus.setTextColor(getColor(R.color.finance_teal))
            }

            totalExpenses <= maxGoal -> {
                tvBudgetStatus.text =
                    "Goal Zone: In target range (${currencyFormatter.format(totalExpenses)} / ${currencyFormatter.format(maxGoal)})"
                tvBudgetStatus.setTextColor(getColor(R.color.finance_green))
            }

            else -> {
                tvBudgetStatus.text =
                    "Goal Zone: Over max range (${currencyFormatter.format(totalExpenses)} / ${currencyFormatter.format(maxGoal)})"
                tvBudgetStatus.setTextColor(getColor(R.color.finance_red))
                progressGoalZone.progress = 100
            }
        }
    }

    /**
     * Displays summary of user financial activity.
     */
    private fun updateMoneyMoves(entries: List<TransactionEntry>) {
        val entryCount = entries.size
        val expenseCount = entries.count { it.entryType == "Expense" }
        val incomeCount = entries.count { it.entryType == "Income" }

        tvMoneyMoves.text = when {
            entryCount == 0 -> {
                "No money moves logged this month. Start tracking to build your streak."
            }

            entryCount == 1 -> {
                "1 money move logged this month. Keep building the habit."
            }

            else -> {
                "$entryCount money moves logged this month: $expenseCount expenses and $incomeCount income boosts."
            }
        }
    }

    /**
     * Calculates financial fitness score based on spending behaviour.
     */
    private fun updateFinancialFitnessScore(
        entries: List<TransactionEntry>,
        totalIncome: Double,
        totalExpenses: Double,
        minGoal: Double?,
        maxGoal: Double?
    ) {
        var score = 100

        if (entries.isEmpty()) {
            score -= 10
        }

        if (totalIncome <= 0.0) {
            score -= 15
        }

        if (totalExpenses > totalIncome && totalIncome > 0.0) {
            score -= 30
        }

        if (maxGoal != null && totalExpenses > maxGoal) {
            score -= 20
        }

        if (minGoal != null && totalExpenses < minGoal && entries.isNotEmpty()) {
            score -= 10
        }

        if (totalExpenses == 0.0 && totalIncome == 0.0) {
            score = 0
        }

        if (score < 0) {
            score = 0
        }

        val formStatus = when {
            score >= 85 -> "Financially Fit"
            score >= 70 -> "Strong Form"
            score >= 50 -> "Needs Training"
            score > 0 -> "Recovery Mode"
            else -> "No Data Yet"
        }

        val statusMessage = when (formStatus) {
            "Financially Fit" -> "You are performing strongly this month."
            "Strong Form" -> "Your money habits are stable. Keep training."
            "Needs Training" -> "Review your spending and stay close to your Goal Zone."
            "Recovery Mode" -> "Focus on reducing high spending areas."
            else -> "Add income, expenses and goals to calculate your score."
        }

        tvFinancialFitness.text = "Fitness Score: $score/100"
        tvFinancialFitnessStatus.text = "Form: $formStatus. $statusMessage"

        when {
            score >= 70 -> tvFinancialFitness.setTextColor(getColor(R.color.finance_green))
            score >= 50 -> tvFinancialFitness.setTextColor(getColor(R.color.finance_teal))
            else -> tvFinancialFitness.setTextColor(getColor(R.color.finance_red))
        }
    }

    /**
     * Provides financial advice based on user behaviour.
     */
    private fun updateSmartTip(
        entries: List<TransactionEntry>,
        totalIncome: Double,
        totalExpenses: Double,
        maxGoal: Double?
    ) {
        if (entries.isEmpty()) {
            tvMoneyTip.text = "Start your first money workout by logging today’s income or expense."
            return
        }

        if (totalIncome <= 0.0) {
            tvMoneyTip.text = "Add your income fuel so Finance Flex can measure your monthly performance."
            return
        }

        if (maxGoal != null && totalExpenses > maxGoal) {
            val topCategory = buildExpenseCategoryTotals(entries).firstOrNull()?.categoryName

            tvMoneyTip.text = if (topCategory != null) {
                "You are over your max Goal Zone. Train smarter by reducing $topCategory spending first."
            } else {
                "You are over your max Goal Zone. Review your biggest expenses this month."
            }

            return
        }

        if (totalExpenses > totalIncome) {
            tvMoneyTip.text = "Your money burn is higher than your income fuel. Try cutting non-essential spending."
            return
        }

        tvMoneyTip.text = "Good form. Keep logging your money moves to maintain your Financial Fitness Score."
    }

    /**
     * Converts expenses into category totals for chart display.
     */
    private fun buildExpenseCategoryTotals(entries: List<TransactionEntry>): List<CategoryTotal> {
        return entries
            .filter { it.entryType == "Expense" }
            .groupBy { it.categoryName }
            .map { groupedItem ->
                CategoryTotal(
                    categoryName = groupedItem.key,
                    totalAmount = groupedItem.value.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    private fun getCurrentMonthStartDate(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun getCurrentMonthEndDate(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.selectedItemId = R.id.nav_dashboard

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true

                R.id.nav_entries -> {
                    val intent = Intent(this, LogEntryActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    true
                }

                R.id.nav_categories -> {
                    val intent = Intent(this, CategoryActivity::class.java)
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
    private fun updateMonthlyChallenges(entries: List<TransactionEntry>) {
        // 1. Grocery Saver (Target R1000)
        val groceryTotal = entries.filter { it.categoryName == "Grocery" && it.entryType == "Expense" }.sumOf { it.amount }
        Log.d("Challenges", "Grocery Saver: ${currencyFormatter.format(groceryTotal)} / R1000")

        // 2. Spend Less (Target R5000)
        val totalExpenses = entries.filter { it.entryType == "Expense" }.sumOf { it.amount }
        Log.d("Challenges", "Spend Less: ${currencyFormatter.format(totalExpenses)} / R5000")

        // 3. Salary Builder (Target R5000 Income)
        val totalIncome = entries.filter { it.entryType == "Income" }.sumOf { it.amount }
        Log.d("Challenges", "Salary Builder: ${currencyFormatter.format(totalIncome)} / R5000")

        // 4. Money Move Streak (Target 10 transactions)
        val moveCount = entries.size
        Log.d("Challenges", "Move Streak: $moveCount / 10")

        // 5. Receipt Master (Target 5 receipts)
        val receiptCount = entries.count { !it.receiptUrl.isNullOrEmpty() }
        Log.d("Challenges", "Receipt Master: $receiptCount / 5")
    }
}