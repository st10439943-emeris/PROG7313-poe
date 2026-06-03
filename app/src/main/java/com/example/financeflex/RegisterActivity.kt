package com.example.financeflex

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflex.data.entities.User
import com.google.firebase.firestore.FirebaseFirestore

/**
 * RegisterActivity
 *
 * This activity allows new users to create an account.
 * Credentials are validated and stored in Firebase Firestore.
 */
class RegisterActivity : AppCompatActivity() {

    private val tag = "FinanceFlexRegister"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Bind UI components
        val edtName = findViewById<EditText>(R.id.edtName)
        val edtSurname = findViewById<EditText>(R.id.edtSurname)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val edtUsername = findViewById<EditText>(R.id.edtUsernameRegister)
        val edtPassword = findViewById<EditText>(R.id.edtPasswordRegister)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvRegisterMessage = findViewById<TextView>(R.id.tvRegisterMessage)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnRegister.setOnClickListener {
            val name = edtName.text.toString().trim()
            val surname = edtSurname.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            // Input Validation
            if (name.isEmpty()) { edtName.error = "Name is required"; return@setOnClickListener }
            if (surname.isEmpty()) { edtSurname.error = "Surname is required"; return@setOnClickListener }
            if (email.isEmpty()) { edtEmail.error = "Email is required"; return@setOnClickListener }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { edtEmail.error = "Enter a valid email"; return@setOnClickListener }
            if (phone.isEmpty()) { edtPhone.error = "Phone number is required"; return@setOnClickListener }
            if (username.isEmpty()) { edtUsername.error = "Username is required"; return@setOnClickListener }
            if (password.isEmpty()) { edtPassword.error = "Password is required"; return@setOnClickListener }
            if (password.length < 4) { edtPassword.error = "Password must be at least 4 chars"; return@setOnClickListener }
            if (password != confirmPassword) { edtConfirmPassword.error = "Passwords do not match"; return@setOnClickListener }

            tvRegisterMessage.text = "Creating account..."

            // Check for existing user in Firestore
            val db = FirebaseFirestore.getInstance()

            db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { usernameDocs ->
                    if (!usernameDocs.isEmpty) {
                        tvRegisterMessage.setTextColor(getColor(R.color.finance_red))
                        tvRegisterMessage.text = "Username already exists"
                        return@addOnSuccessListener
                    }

                    // Check for email
                    db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { emailDocs ->
                            if (!emailDocs.isEmpty) {
                                tvRegisterMessage.setTextColor(getColor(R.color.finance_red))
                                tvRegisterMessage.text = "Email already exists"
                                return@addOnSuccessListener
                            }

                            // If no duplicates, create new user
                            val newUser = User(
                                name = name,
                                surname = surname,
                                email = email,
                                phone = phone,
                                username = username,
                                password = password
                            )

                            db.collection("users")
                                .add(newUser)
                                .addOnSuccessListener { docRef ->
                                    // Update user with auto-generated ID
                                    docRef.update("userId", docRef.id)

                                    tvRegisterMessage.setTextColor(getColor(R.color.finance_green))
                                    tvRegisterMessage.text = "Account created successfully."

                                    edtName.text.clear(); edtSurname.text.clear(); edtEmail.text.clear()
                                    edtPhone.text.clear(); edtUsername.text.clear(); edtPassword.text.clear()
                                    edtConfirmPassword.text.clear()
                                }
                                .addOnFailureListener { e ->
                                    tvRegisterMessage.setTextColor(getColor(R.color.finance_red))
                                    tvRegisterMessage.text = "Registration failed."
                                    Log.e(tag, "Error saving user", e)
                                }
                        }
                }
                .addOnFailureListener { e ->
                    tvRegisterMessage.setTextColor(getColor(R.color.finance_red))
                    tvRegisterMessage.text = "Registration error."
                    Log.e(tag, "Error querying database", e)
                }
        }

        tvBackToLogin.setOnClickListener { finish() }
    }
}