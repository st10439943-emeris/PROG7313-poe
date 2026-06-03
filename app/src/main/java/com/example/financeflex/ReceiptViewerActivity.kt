package com.example.financeflex

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.financeflex.data.entities.TransactionEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

/**
 * This activity allows users to view and download receipt images
 * associated with their financial transactions from the cloud.
 */
class ReceiptViewerActivity : AppCompatActivity() {

    private val tag = "FinanceFlexReceiptViewer"
    private var userId: String = "-1"

    //UI Components
    private lateinit var spinnerReceipts: Spinner
    private lateinit var imgReceiptFullPreview: ImageView
    private lateinit var tvMessage: TextView

    //List of transaction entries containing receipts
    private var receiptEntries: List<TransactionEntry> = emptyList()

    // UPDATED: Now tracks the internet URL instead of a local URI
    private var selectedReceiptUrl: String? = null

    private val currencyFormatter: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    /**
     * Activity Result Launcher used to create a document
     * and save the downloaded cloud receipt image.
     */
    private val downloadReceiptLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destinationUri: Uri? ->
        if (destinationUri != null) {
            saveReceiptToSelectedLocation(destinationUri)
        } else {
            tvMessage.setTextColor(getColor(R.color.finance_gray))
            tvMessage.text = "Download cancelled."
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_viewer)

        userId = intent.getStringExtra("userId") ?: "-1"

        //Bind UI components
        spinnerReceipts = findViewById(R.id.spinnerReceipts)
        imgReceiptFullPreview = findViewById(R.id.imgReceiptFullPreview)
        val btnDownloadReceipt = findViewById<Button>(R.id.btnDownloadReceipt)
        val tvBackToReports = findViewById<TextView>(R.id.tvBackToReports)
        tvMessage = findViewById(R.id.tvReceiptViewerMessage)

        loadReceiptEntries()

        spinnerReceipts.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < receiptEntries.size) {
                    val entry = receiptEntries[position]
                    selectedReceiptUrl = entry.receiptUrl // Check the cloud URL

                    if (!selectedReceiptUrl.isNullOrEmpty()) {
                        tvMessage.setTextColor(getColor(R.color.finance_gray))
                        tvMessage.text = "Loading cloud image..."

                        // Download the image in the background so the app doesn't freeze
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val url = URL(selectedReceiptUrl)
                                val bitmap = BitmapFactory.decodeStream(url.openConnection().inputStream)

                                // Switch back to the main screen to show the image
                                withContext(Dispatchers.Main) {
                                    imgReceiptFullPreview.setImageBitmap(bitmap)
                                    tvMessage.text = ""
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    tvMessage.setTextColor(getColor(R.color.finance_red))
                                    tvMessage.text = "Failed to load cloud image."
                                }
                            }
                        }
                    } else {
                        imgReceiptFullPreview.setImageDrawable(null)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

        btnDownloadReceipt.setOnClickListener {
            if (selectedReceiptUrl.isNullOrEmpty()) {
                tvMessage.setTextColor(getColor(R.color.finance_red))
                tvMessage.text = "No receipt selected."
            } else {
                val fileName = "finance_flex_receipt_${System.currentTimeMillis()}.jpg"
                downloadReceiptLauncher.launch(fileName)
            }
        }

        tvBackToReports.setOnClickListener {
            finish()
        }
    }

    /**
     * Loads receipt entries directly from Firestore.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadReceiptEntries() {
        if (userId == "-1") {
            tvMessage.setTextColor(getColor(R.color.finance_red))
            tvMessage.text = "User not found. Please login again."
            return
        }

        tvMessage.setTextColor(getColor(R.color.finance_gray))
        tvMessage.text = "Loading receipts from cloud..."

        FirebaseManager.firestore.collection("transactions")
            .whereEqualTo("userId", userId.toString())
            .get()
            .addOnSuccessListener { documents ->
                val allEntries = documents.toObjects(TransactionEntry::class.java)

                // Filter to ONLY show entries that actually have a receipt URL attached
                receiptEntries = allEntries.filter { !it.receiptUrl.isNullOrEmpty() }

                if (receiptEntries.isEmpty()) {
                    tvMessage.setTextColor(getColor(R.color.finance_gray))
                    tvMessage.text = "No cloud receipts found."
                    imgReceiptFullPreview.setImageDrawable(null)
                    return@addOnSuccessListener
                }

                val receiptLabels = receiptEntries.map { entry ->
                    "${entry.entryDate} - ${entry.categoryName} - ${currencyFormatter.format(entry.amount)}"
                }

                val adapter = ArrayAdapter(
                    this@ReceiptViewerActivity,
                    android.R.layout.simple_spinner_item,
                    receiptLabels
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerReceipts.adapter = adapter
                tvMessage.text = ""

                Log.d(tag, "Loaded ${receiptEntries.size} cloud receipt entries")
            }
            .addOnFailureListener { exception ->
                tvMessage.setTextColor(getColor(R.color.finance_red))
                tvMessage.text = "Could not load receipts."
                Log.e(tag, "Error loading receipts", exception)
            }
    }

    /**
     * Downloads the image from the cloud URL and saves it to the user's phone.
     */
    private fun saveReceiptToSelectedLocation(destinationUri: Uri) {
        tvMessage.setTextColor(getColor(R.color.finance_gray))
        tvMessage.text = "Downloading receipt..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sourceUrl = URL(selectedReceiptUrl)
                val inputStream = sourceUrl.openStream()
                val outputStream = contentResolver.openOutputStream(destinationUri)

                if (inputStream != null && outputStream != null) {
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        tvMessage.setTextColor(getColor(R.color.finance_green))
                        tvMessage.text = "Receipt downloaded successfully."
                    }
                }
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    tvMessage.setTextColor(getColor(R.color.finance_red))
                    tvMessage.text = "Could not download receipt."
                    Log.e(tag, "Error downloading receipt", exception)
                }
            }
        }
    }
}