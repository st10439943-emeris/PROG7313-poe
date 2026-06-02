package com.example.financeflex

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflex.data.entities.User

class AccountSettingsActivity : AppCompatActivity() {

    private val tag = "FinanceFlexSettings"
    private var userId: String = "-1"
    private var currentUser: User? = null

    private lateinit var edtName: EditText
    private lateinit var edtSurname: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtUsername: EditText
    private lateinit var tvMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)

        userId = intent.getStringExtra("userId") ?: "-1"

        edtName = findViewById(R.id.edtSettingsName)
        edtSurname = findViewById(R.id.edtSettingsSurname)
        edtEmail = findViewById(R.id.edtSettingsEmail)
        edtPhone = findViewById(R.id.edtSettingsPhone)
        edtUsername = findViewById(R.id.edtSettingsUsername)

        val btnSave = findViewById<Button>(R.id.btnSaveAccountSettings)
        val tvBackToDashboard = findViewById<TextView>(R.id.tvBackToDashboard)
        tvMessage = findViewById(R.id.tvAccountSettingsMessage)

        loadUserDetails()

        btnSave.setOnClickListener {
            saveAccountSettings()
        }

        tvBackToDashboard.setOnClickListener {
            finish()
        }
    }

    private fun loadUserDetails() {
        if (userId == "-1") {
            tvMessage.setTextColor(getColor(R.color.finance_red))
            tvMessage.text = "User missing. Login again."
            return
        }

        FirebaseManager.firestore.collection("users")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    tvMessage.setTextColor(getColor(R.color.finance_red))
                    tvMessage.text = "Account details load failed."
                    return@addOnSuccessListener
                }

                val user = querySnapshot.documents[0].toObject(User::class.java)
                currentUser = user

                edtName.setText(user?.name)
                edtSurname.setText(user?.surname)
                edtEmail.setText(user?.email)
                edtPhone.setText(user?.phone)
                edtUsername.setText(user?.username)
            }
            .addOnFailureListener { exception ->
                tvMessage.setTextColor(getColor(R.color.finance_red))
                tvMessage.text = "Account settings load failed."
                Log.e(tag, "Error loading account settings", exception)
            }
    }

    private fun saveAccountSettings() {
        val name = edtName.text.toString().trim()
        val surname = edtSurname.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val username = edtUsername.text.toString().trim()

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || phone.isEmpty() || username.isEmpty()) {
            tvMessage.setTextColor(getColor(R.color.finance_red))
            tvMessage.text = "All fields are required."
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Enter valid email."
            return
        }

        FirebaseManager.firestore.collection("users")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) return@addOnSuccessListener
                val docId = documents.documents[0].id

                FirebaseManager.firestore.collection("users").document(docId)
                    .update(
                        mapOf(
                            "name" to name,
                            "surname" to surname,
                            "email" to email,
                            "phone" to phone,
                            "username" to username
                        )
                    )
                    .addOnSuccessListener {
                        tvMessage.setTextColor(getColor(R.color.finance_green))
                        tvMessage.text = "Account updated successfully."
                    }
                    .addOnFailureListener {
                        tvMessage.setTextColor(getColor(R.color.finance_red))
                        tvMessage.text = "Update failed."
                    }
            }
    }
}