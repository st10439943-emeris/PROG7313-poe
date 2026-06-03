package com.example.financeflex

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.financeflex.data.entities.Category
import com.example.financeflex.data.entities.TransactionEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LogEntryActivity : AppCompatActivity() {

    private val tag = "FinanceFlexEntry"
    private var userId: String = "-1"

    private lateinit var edtAmount: EditText
    private lateinit var spinnerCategories: Spinner
    private lateinit var btnDate: Button
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var edtDescription: EditText

    private lateinit var btnTakeReceiptPhoto: Button
    private lateinit var btnSelectReceipt: Button
    private lateinit var imgReceiptPreview: ImageView
    private lateinit var tvReceiptStatus: TextView
    private lateinit var tvEntryMessage: TextView

    private var selectedDate = ""
    private var selectedStartTime = ""
    private var selectedEndTime = ""
    private var selectedReceiptUri: String? = null
    private var cameraImageUri: Uri? = null

    private var categoryList: List<Category> = emptyList()

    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedReceiptUri = uri.toString()
            imgReceiptPreview.setImageURI(uri)
            imgReceiptPreview.visibility = ImageView.VISIBLE
            tvReceiptStatus.text = "Receipt selected"
            tvReceiptStatus.setTextColor(getColor(R.color.finance_green))
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            selectedReceiptUri = cameraImageUri.toString()
            imgReceiptPreview.setImageURI(cameraImageUri)
            imgReceiptPreview.visibility = ImageView.VISIBLE
            tvReceiptStatus.text = "Receipt captured"
            tvReceiptStatus.setTextColor(getColor(R.color.finance_green))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_entry)

        userId = intent.getStringExtra("userId") ?: "-1"

        edtAmount = findViewById(R.id.edtAmount)
        spinnerCategories = findViewById(R.id.spinnerCategories)
        btnDate = findViewById(R.id.btnDate)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        edtDescription = findViewById(R.id.edtDescription)
        btnTakeReceiptPhoto = findViewById(R.id.btnTakeReceiptPhoto)
        btnSelectReceipt = findViewById(R.id.btnSelectReceipt)
        imgReceiptPreview = findViewById(R.id.imgReceiptPreview)
        tvReceiptStatus = findViewById(R.id.tvReceiptStatus)

        val btnSaveEntry = findViewById<Button>(R.id.btnSaveEntry)
        tvEntryMessage = findViewById(R.id.tvEntryMessage)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        setDefaultDateAndTimes()
        loadCategories()

        btnDate.setOnClickListener { showDatePicker() }
        btnStartTime.setOnClickListener { showTimePicker(true) }
        btnEndTime.setOnClickListener { showTimePicker(false) }
        btnTakeReceiptPhoto.setOnClickListener { takeReceiptPhoto() }
        btnSelectReceipt.setOnClickListener { galleryPickerLauncher.launch("image/*") }
        btnSaveEntry.setOnClickListener { saveEntry() }

        setupBottomNavigation(bottomNavigation)
    }

    private fun takeReceiptPhoto() {
        val imageFile = createReceiptImageFile()
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun createReceiptImageFile(): File {
        val receiptDirectory = File(cacheDir, "receipt_images")
        if (!receiptDirectory.exists()) receiptDirectory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().time)
        return File(receiptDirectory, "receipt_$timestamp.jpg")
    }

    private fun setDefaultDateAndTimes() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        selectedDate = dateFormat.format(calendar.time)
        selectedStartTime = timeFormat.format(calendar.time)
        selectedEndTime = timeFormat.format(calendar.time)
        btnDate.text = "Date: $selectedDate"
        btnStartTime.text = "Start Time: $selectedStartTime"
        btnEndTime.text = "End Time: $selectedEndTime"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadCategories() {
        if (userId == "-1") {
            tvEntryMessage.text = "User not found."
            return
        }

        FirebaseManager.firestore.collection("categories")
            .whereEqualTo("userId", userId.toString())
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    insertDefaultCategories()
                } else {
                    categoryList = documents.toObjects(Category::class.java)
                    updateSpinner()
                }
            }
            .addOnFailureListener { e ->
                tvEntryMessage.text = "Could not load categories."
                Log.e(tag, "Error", e)
            }
    }

    private fun updateSpinner() {
        val categoryNames = categoryList.map { "${it.categoryType}: ${it.categoryName}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun insertDefaultCategories() {
        val defaultCategories = listOf(
            Category(userId = userId.toString(), categoryName = "Food", categoryType = "Expense", iconName = "food", isDefault = true),
            Category(userId = userId.toString(), categoryName = "Transport", categoryType = "Expense", iconName = "car", isDefault = true),
            Category(userId = userId.toString(), categoryName = "Entertainment", categoryType = "Expense", iconName = "fun", isDefault = true),
            Category(userId = userId.toString(), categoryName = "Bills", categoryType = "Expense", iconName = "bill", isDefault = true),
            Category(userId = userId.toString(), categoryName = "Health", categoryType = "Expense", iconName = "health", isDefault = true),
            Category(userId = userId.toString(), categoryName = "Salary", categoryType = "Income", iconName = "salary", isDefault = true),
            Category(userId = userId.toString(), categoryName = "Allowance", categoryType = "Income", iconName = "allowance", isDefault = true)
        )

        val batch = FirebaseManager.firestore.batch()
        val colRef = FirebaseManager.firestore.collection("categories")
        defaultCategories.forEach { batch.set(colRef.document(), it) }
        batch.commit().addOnSuccessListener { loadCategories() }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
            btnDate.text = "Date: $selectedDate"
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val c = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            val time = String.format("%02d:%02d", h, m)
            if (isStartTime) {
                selectedStartTime = time
                btnStartTime.text = "Start Time: $selectedStartTime"
            } else {
                selectedEndTime = time
                btnEndTime.text = "End Time: $selectedEndTime"
            }
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }

    private fun saveEntry() {
        val amountText = edtAmount.text.toString().trim()
        val desc = edtDescription.text.toString().trim()
        if (amountText.isEmpty() || desc.isEmpty()) return

        val amount = amountText.toDoubleOrNull() ?: return
        val cat = categoryList[spinnerCategories.selectedItemPosition]

        fun finishSave(url: String?) {
            val entry = TransactionEntry(
                userId = userId.toString(),
                categoryId = cat.categoryId,
                categoryName = cat.categoryName,
                entryType = cat.categoryType,
                amount = amount,
                entryDate = selectedDate,
                startTime = selectedStartTime,
                endTime = selectedEndTime,
                description = desc,
                receiptUrl = url
            )
            FirebaseManager.firestore.collection("transactions").add(entry)
                .addOnSuccessListener { tvEntryMessage.text = "Saved." }
        }

        if (selectedReceiptUri != null) {
            // Save the local phone URI directly to Firestore instead of uploading.
            // This bypasses the Firebase Storage Blaze paywall entirely.
            finishSave(selectedReceiptUri)
        } else {
            finishSave(null)
        }
    }

    private fun setupBottomNavigation(nav: BottomNavigationView) {
        nav.selectedItemId = R.id.nav_entries
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { finish(); true }
                R.id.nav_entries -> true
                R.id.nav_categories -> { startActivity(Intent(this, CategoryActivity::class.java).putExtra("userId", userId)); true }
                R.id.nav_reports -> { startActivity(Intent(this, ViewEntriesActivity::class.java).putExtra("userId", userId)); true }
                R.id.nav_goals -> { startActivity(Intent(this, BudgetGoalActivity::class.java).putExtra("userId", userId)); true }
                else -> false
            }
        }
    }
}