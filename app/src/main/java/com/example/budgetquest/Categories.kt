package com.example.budgetquest

import Data.Database.AppDatabase
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.Category
import kotlinx.coroutines.launch

class Categories : AppCompatActivity() {

    private lateinit var edtCategoryName: EditText
    private lateinit var edtCategoryLimit: EditText
    private lateinit var btnSaveCategory: Button
    private lateinit var tvCategoryList: TextView

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        db = AppDatabase.getDatabase(this)

        // typecasting
        edtCategoryName = findViewById(R.id.edtCategoryName)
        edtCategoryLimit = findViewById(R.id.edtCategoryLimit)
        btnSaveCategory = findViewById(R.id.btnSaveCategory)
        tvCategoryList = findViewById(R.id.tvCategoryList)

        btnSaveCategory.setOnClickListener {
            saveCategory()
        }

        setupBottomNav()
        loadCategories()
    }

    private fun saveCategory() {
        val categoryName = edtCategoryName.text.toString().trim()
        val limitText = edtCategoryLimit.text.toString().trim()

        // validation
        if (categoryName.isEmpty() || limitText.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter category name and monthly limit",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val monthlyLimit = limitText.toDoubleOrNull()

        if (monthlyLimit == null || monthlyLimit <= 0) {
            Toast.makeText(
                this,
                "Please enter a valid monthly limit",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            val existingCategory =
                db.categoryDao().getCategoryByName(categoryName)

            if (existingCategory != null) {
                runOnUiThread {
                    Toast.makeText(
                        this@Categories,
                        "Category already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            val category = Category(
                name = categoryName,
                monthlyLimit = monthlyLimit
            )

            db.categoryDao().insertCategory(category)

            runOnUiThread {
                Toast.makeText(
                    this@Categories,
                    "Category saved successfully",
                    Toast.LENGTH_SHORT
                ).show()

                edtCategoryName.text.clear()
                edtCategoryLimit.text.clear()
                loadCategories()
            }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategories()

            runOnUiThread {
                if (categories.isEmpty()) {
                    tvCategoryList.text = "No categories added yet."
                } else {
                    tvCategoryList.text =
                        categories.joinToString(separator = "\n") {
                            "• ${it.name} - Monthly Limit: R${it.monthlyLimit}"
                        }
                }
            }
        }
    }

    private fun setupBottomNav() {
        val navHome = findViewById<TextView>(R.id.navHome)
        val navCategories = findViewById<TextView>(R.id.navCategories)
        val navAddExpense = findViewById<TextView>(R.id.navAddExpense)
        val navGoals = findViewById<TextView>(R.id.navGoals)
        val navProfile = findViewById<TextView>(R.id.navProfile)

        navHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        navCategories.setOnClickListener {
            Toast.makeText(
                this,
                "You are already on the Categories screen",
                Toast.LENGTH_SHORT
            ).show()
        }

        navAddExpense.setOnClickListener {
            startActivity(Intent(this, Expenses::class.java))
        }

        navGoals.setOnClickListener {
            startActivity(Intent(this, MonthlyGoals::class.java))
        }

        navProfile.setOnClickListener {
            Toast.makeText(
                this,
                "Profile screen will be added soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}