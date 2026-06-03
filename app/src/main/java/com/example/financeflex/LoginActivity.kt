package com.example.financeflex

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflex.data.entities.User

/*
 * LoginActivity handles user authentication for the FinanceFlex application.
 * Credentials are now validated against Firebase Firestore.
 */
class LoginActivity : AppCompatActivity() {

    private val tag = "FinanceFlexLogin"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvLoginMessage = findViewById<TextView>(R.id.tvLoginMessage)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (username.isEmpty()) {
                edtUsername.error = "Username is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                edtPassword.error = "Password is required"
                return@setOnClickListener
            }

            // Query Firestore to verify credentials
            FirebaseManager.firestore.collection("users")
                .whereEqualTo("username", username)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // Login successful - grab the first user document found
                        val user = documents.documents[0].toObject(User::class.java)

                        if (user != null) {
                            tvLoginMessage.setTextColor(getColor(R.color.finance_green))
                            tvLoginMessage.text = "Login successful"
                            Log.d(tag, "Firestore login successful for: $username")

                            val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                            intent.putExtra("username", username)
                            intent.putExtra("userId", user.userId)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // No user found with those credentials
                        tvLoginMessage.setTextColor(getColor(R.color.finance_red))
                        tvLoginMessage.text = "Incorrect username or password"
                        Log.d(tag, "Login failed for: $username")
                    }
                }
                .addOnFailureListener { exception ->
                    tvLoginMessage.setTextColor(getColor(R.color.finance_red))
                    tvLoginMessage.text = "Login failed. Please try again."
                    Log.e(tag, "Firestore login error", exception)
                }
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            tvLoginMessage.setTextColor(getColor(R.color.finance_gray))
            tvLoginMessage.text = "Reset Password screen coming later"
        }
    }
}