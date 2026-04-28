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
import android.graphics.Typeface
import android.widget.LinearLayout

class Categories : AppCompatActivity() {

    private lateinit var edtCategoryName: EditText
    private lateinit var edtCategoryLimit: EditText
    private lateinit var btnSaveCategory: Button

    private lateinit var categoryListContainer: LinearLayout
    private var editingCategory: Category? = null

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        db = AppDatabase.getDatabase(this)

        // typecasting
        edtCategoryName = findViewById(R.id.edtCategoryName)
        edtCategoryLimit = findViewById(R.id.edtCategoryLimit)
        btnSaveCategory = findViewById(R.id.btnSaveCategory)
        categoryListContainer = findViewById(R.id.categoryListContainer)
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
        val categoryBeingEdited = editingCategory

        if (categoryBeingEdited != null) {
            val updatedCategory = categoryBeingEdited.copy(
                name = categoryName,
                monthlyLimit = monthlyLimit
            )

            lifecycleScope.launch {
                db.categoryDao().updateCategory(updatedCategory)

                runOnUiThread {
                    Toast.makeText(this@Categories, "Category updated successfully", Toast.LENGTH_SHORT).show()
                    edtCategoryName.text.clear()
                    edtCategoryLimit.text.clear()
                    btnSaveCategory.text = "Save Category"
                    editingCategory = null
                    loadCategories()
                }
            }

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
                categoryListContainer.removeAllViews()

                if (categories.isEmpty()) {
                    val emptyText = TextView(this@Categories)
                    emptyText.text = "No categories added yet."
                    emptyText.textSize = 16f
                    emptyText.setTextColor(android.graphics.Color.parseColor("#263238"))
                    categoryListContainer.addView(emptyText)
                } else {
                    categories.forEach { category ->
                        addCategoryBubble(category)
                    }
                }
            }
        }
    }
    private fun addCategoryBubble(category: Category) {
        val bubble = LinearLayout(this)
        bubble.orientation = LinearLayout.VERTICAL
        bubble.setPadding(18, 18, 18, 18)
        bubble.setBackgroundResource(R.drawable.login_card_bg)

        val bubbleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        bubbleParams.setMargins(0, 0, 0, 18)
        bubble.layoutParams = bubbleParams

        val title = TextView(this)
        title.text = category.name
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)

        val limit = TextView(this)
        limit.text = "Monthly Limit: R${category.monthlyLimit}"
        limit.textSize = 15f

        val buttonRow = LinearLayout(this)
        buttonRow.orientation = LinearLayout.HORIZONTAL

        val editButton = Button(this)
        editButton.text = "Edit"
        editButton.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val deleteButton = Button(this)
        deleteButton.text = "Delete"
        deleteButton.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        editButton.setOnClickListener {
            editingCategory = category
            edtCategoryName.setText(category.name)
            edtCategoryLimit.setText(category.monthlyLimit.toString())
            btnSaveCategory.text = "Update Category"
        }

        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                db.categoryDao().deleteCategory(category)

                runOnUiThread {
                    Toast.makeText(this@Categories, "Category deleted", Toast.LENGTH_SHORT).show()
                    loadCategories()
                }
            }
        }

        buttonRow.addView(editButton)
        buttonRow.addView(deleteButton)

        bubble.addView(title)
        bubble.addView(limit)
        bubble.addView(buttonRow)

        categoryListContainer.addView(bubble)
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