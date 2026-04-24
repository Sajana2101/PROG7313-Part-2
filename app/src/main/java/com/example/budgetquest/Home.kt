package com.example.budgetquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Home : AppCompatActivity() {

    private lateinit var btnExpenses: Button
    private lateinit var btnGoals: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        btnExpenses = findViewById(R.id.btnExpenses)
        btnGoals = findViewById(R.id.btnGoals)
        btnLogout = findViewById(R.id.btnLogout)

        btnExpenses.setOnClickListener {
            Toast.makeText(this, "Opening expenses entry screen", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Expenses::class.java)
            startActivity(intent)
        }

        btnGoals.setOnClickListener {
            Toast.makeText(this, "Opening monthly budget goals screen", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MonthlyGoals::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            Toast.makeText(this, "Logging out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}