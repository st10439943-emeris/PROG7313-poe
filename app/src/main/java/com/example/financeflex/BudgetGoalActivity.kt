package com.example.financeflex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflex.data.entities.BudgetGoal
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Activity manages monthly budget goals.
 * Users set spending limits for the current month.
 * The system stores data in Firebase.
 */
class BudgetGoalActivity : AppCompatActivity() {

    private val tag = "FinanceFlexGoals"
    private var userId: String = "-1"

    private lateinit var edtMinGoal: EditText
    private lateinit var edtMaxGoal: EditText
    private lateinit var tvMinGoalPreview: TextView
    private lateinit var tvMaxGoalPreview: TextView
    private lateinit var tvGoalMessage: TextView

    private val currencyFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_goal)

        userId = intent.getStringExtra("userId") ?: "-1"

        val tvGoalMonth = findViewById<TextView>(R.id.tvGoalMonth)
        edtMinGoal = findViewById(R.id.edtMinGoal)
        edtMaxGoal = findViewById(R.id.edtMaxGoal)

        val seekMinGoal = findViewById<SeekBar>(R.id.seekMinGoal)
        val seekMaxGoal = findViewById<SeekBar>(R.id.seekMaxGoal)

        tvMinGoalPreview = findViewById(R.id.tvMinGoalPreview)
        tvMaxGoalPreview = findViewById(R.id.tvMaxGoalPreview)

        val btnSaveGoals = findViewById<Button>(R.id.btnSaveGoals)
        tvGoalMessage = findViewById(R.id.tvGoalMessage)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        tvGoalMonth.text = "$monthName $year"

        setupSeekBars(seekMinGoal, seekMaxGoal)
        loadExistingGoal(month, year)

        btnSaveGoals.setOnClickListener {
            saveBudgetGoal(month, year)
        }

        setupBottomNavigation(bottomNavigation)
    }

    // Configures SeekBar controls for real-time feedback.
    private fun setupSeekBars(seekMinGoal: SeekBar, seekMaxGoal: SeekBar) {
        seekMinGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    edtMinGoal.setText(progress.toString())
                    tvMinGoalPreview.text =
                        "Minimum preview: ${currencyFormatter.format(progress.toDouble())}"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekMaxGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    edtMaxGoal.setText(progress.toString())
                    tvMaxGoalPreview.text =
                        "Maximum preview: ${currencyFormatter.format(progress.toDouble())}"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        edtMinGoal.setText(seekMinGoal.progress.toString())
        edtMaxGoal.setText(seekMaxGoal.progress.toString())

        tvMinGoalPreview.text =
            "Minimum preview: ${currencyFormatter.format(seekMinGoal.progress.toDouble())}"

        tvMaxGoalPreview.text =
            "Maximum preview: ${currencyFormatter.format(seekMaxGoal.progress.toDouble())}"
    }

    // Loads existing budget goal from Firebase.
    private fun loadExistingGoal(month: Int, year: Int) {
        if (userId == "-1") {
            tvGoalMessage.setTextColor(getColor(R.color.finance_red))
            tvGoalMessage.text = "User not found. Login again."
            return
        }

        tvGoalMessage.setTextColor(getColor(R.color.finance_gray))
        tvGoalMessage.text = "Loading cloud goals."

        FirebaseManager.firestore.collection("budgetGoals")
            .whereEqualTo("userId", userId.toString())
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .get()
            .addOnSuccessListener { documents ->
                tvGoalMessage.text = ""
                if (!documents.isEmpty) {
                    val existingGoal = documents.documents[0].toObject(BudgetGoal::class.java)

                    if (existingGoal != null) {
                        edtMinGoal.setText(existingGoal.minGoal.toInt().toString())
                        edtMaxGoal.setText(existingGoal.maxGoal.toInt().toString())

                        tvMinGoalPreview.text =
                            "Minimum preview: ${currencyFormatter.format(existingGoal.minGoal)}"

                        tvMaxGoalPreview.text =
                            "Maximum preview: ${currencyFormatter.format(existingGoal.maxGoal)}"
                    }
                }
            }
            .addOnFailureListener { exception ->
                tvGoalMessage.setTextColor(getColor(R.color.finance_red))
                tvGoalMessage.text = "Could not load cloud goals."
                Log.e(tag, "Error loading cloud budget goal", exception)
            }
    }

    // Validates and saves data to Firebase.
    private fun saveBudgetGoal(month: Int, year: Int) {
        val minGoalText = edtMinGoal.text.toString().trim()
        val maxGoalText = edtMaxGoal.text.toString().trim()

        if (userId == "-1") {
            tvGoalMessage.setTextColor(getColor(R.color.finance_red))
            tvGoalMessage.text = "User not found. Login again."
            return
        }

        if (minGoalText.isEmpty() || maxGoalText.isEmpty()) {
            tvGoalMessage.setTextColor(getColor(R.color.finance_red))
            tvGoalMessage.text = "Goals are required."
            return
        }

        val minGoal = minGoalText.toDoubleOrNull() ?: 0.0
        val maxGoal = maxGoalText.toDoubleOrNull() ?: 0.0

        if (minGoal >= maxGoal) {
            tvGoalMessage.setTextColor(getColor(R.color.finance_red))
            tvGoalMessage.text = "Min goal must be less than max."
            return
        }

        tvGoalMessage.setTextColor(getColor(R.color.finance_gray))
        tvGoalMessage.text = "Saving to cloud."

        val collectionRef = FirebaseManager.firestore.collection("budgetGoals")

        collectionRef
            .whereEqualTo("userId", userId.toString())
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val newGoal = BudgetGoal(
                        userId = userId.toString(),
                        month = month,
                        year = year,
                        minGoal = minGoal,
                        maxGoal = maxGoal
                    )

                    collectionRef.add(newGoal)
                        .addOnSuccessListener { docRef ->
                            docRef.update("documentId", docRef.id)
                            tvGoalMessage.setTextColor(getColor(R.color.finance_green))
                            tvGoalMessage.text = "Budget goals saved."
                        }
                } else {
                    val existingDocumentId = documents.documents[0].id
                    collectionRef.document(existingDocumentId)
                        .update(mapOf("minGoal" to minGoal, "maxGoal" to maxGoal))
                        .addOnSuccessListener {
                            tvGoalMessage.setTextColor(getColor(R.color.finance_green))
                            tvGoalMessage.text = "Budget goals updated."
                        }
                }
            }
            .addOnFailureListener { e ->
                tvGoalMessage.setTextColor(getColor(R.color.finance_red))
                tvGoalMessage.text = "Save failed."
                Log.e(tag, "Error saving goal", e)
            }
    }

    // Handles navigation.
    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.selectedItemId = R.id.nav_goals
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    finish()
                    true
                }
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
                R.id.nav_goals -> true
                else -> false
            }
        }
    }
}