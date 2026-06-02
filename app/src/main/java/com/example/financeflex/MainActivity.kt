package com.example.financeflex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Entry point for the FinanceFlex application.
 * Navigates users to the Login screen.
 */
class MainActivity : AppCompatActivity() {

    private val tag = "FinanceFlex"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        btnGetStarted.setOnClickListener {
            Log.d(tag, "Get Started clicked - opening LoginActivity")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        Log.d(tag, "Welcome screen opened successfully")
    }
}