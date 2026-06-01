package com.example.budgetquest

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetquest.firebase.FirebaseCategory
import com.example.budgetquest.firebase.FirebaseRepository
import java.util.Locale

class Categories : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var edtCategoryName: EditText
    private lateinit var edtCategoryLimit: EditText
    private lateinit var btnSaveCategory: Button
    private lateinit var categoryListContainer: LinearLayout

    private var editingCategory: FirebaseCategory? = null
    private var userUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

        edtCategoryName = findViewById(R.id.edtCategoryName)
        edtCategoryLimit = findViewById(R.id.edtCategoryLimit)
        btnSaveCategory = findViewById(R.id.btnSaveCategory)
        categoryListContainer = findViewById(R.id.categoryListContainer)

        btnSaveCategory.setOnClickListener {
            saveCategory()
        }

        NavigationHelper.setupBottomNavigation(
            activity = this,
            userUid = userUid,
            currentPage = "Categories"
        )

        loadCategories()
    }

    private fun saveCategory() {
        val categoryName = edtCategoryName.text.toString().trim()
        val limitText = edtCategoryLimit.text.toString().trim()
        val monthlyLimit = limitText.toDoubleOrNull()

        if (categoryName.isEmpty() || limitText.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter category name and monthly limit.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (monthlyLimit == null || monthlyLimit <= 0) {
            Toast.makeText(
                this,
                "Please enter a positive monthly limit.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnSaveCategory.isEnabled = false

        repository.getCategoryByName(
            uid = userUid,
            categoryName = categoryName,
            onSuccess = { existingCategory ->
                val categoryBeingEdited = editingCategory

                if (
                    existingCategory != null &&
                    existingCategory.id != categoryBeingEdited?.id
                ) {
                    btnSaveCategory.isEnabled = true

                    Toast.makeText(
                        this,
                        "Category already exists.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@getCategoryByName
                }

                val categoryToSave = if (categoryBeingEdited == null) {
                    FirebaseCategory(
                        name = categoryName,
                        monthlyLimit = monthlyLimit
                    )
                } else {
                    categoryBeingEdited.copy(
                        name = categoryName,
                        monthlyLimit = monthlyLimit
                    )
                }

                saveCategoryToFirebase(
                    category = categoryToSave,
                    isEditing = categoryBeingEdited != null
                )
            },
            onError = { errorMessage ->
                btnSaveCategory.isEnabled = true

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun saveCategoryToFirebase(
        category: FirebaseCategory,
        isEditing: Boolean
    ) {
        repository.saveCategory(
            uid = userUid,
            category = category,
            onSuccess = {
                btnSaveCategory.isEnabled = true

                val successMessage = if (isEditing) {
                    "Category updated successfully."
                } else {
                    "Category saved successfully."
                }

                Toast.makeText(
                    this,
                    successMessage,
                    Toast.LENGTH_SHORT
                ).show()

                clearCategoryForm()
                loadCategories()
            },
            onError = { errorMessage ->
                btnSaveCategory.isEnabled = true

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun loadCategories() {
        repository.getCategories(
            uid = userUid,
            onSuccess = { categories ->
                categoryListContainer.removeAllViews()

                if (categories.isEmpty()) {
                    showEmptyCategoryMessage()
                } else {
                    categories.forEach { category ->
                        addCategoryBubble(category)
                    }
                }
            },
            onError = { errorMessage ->
                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun showEmptyCategoryMessage() {
        val emptyText = TextView(this)
        emptyText.text = "No categories added yet."
        emptyText.textSize = 16f
        emptyText.setTextColor(Color.parseColor("#263238"))

        categoryListContainer.addView(emptyText)
    }

    private fun addCategoryBubble(category: FirebaseCategory) {
        val bubble = LinearLayout(this)
        bubble.orientation = LinearLayout.VERTICAL
        bubble.setPadding(dp(18), dp(18), dp(18), dp(18))
        bubble.setBackgroundResource(R.drawable.login_card_bg)

        val bubbleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        bubbleParams.setMargins(0, 0, 0, dp(18))
        bubble.layoutParams = bubbleParams

        val title = TextView(this)
        title.text = category.name
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.parseColor("#263238"))

        val limit = TextView(this)
        limit.text = "Monthly Limit: ${formatMoney(category.monthlyLimit)}"
        limit.textSize = 15f
        limit.setTextColor(Color.parseColor("#546E7A"))
        limit.setPadding(0, dp(6), 0, dp(12))

        val buttonRow = LinearLayout(this)
        buttonRow.orientation = LinearLayout.HORIZONTAL

        val editButton = Button(this)
        editButton.text = "Edit"
        editButton.isAllCaps = false

        val editButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        editButtonParams.setMargins(0, 0, dp(6), 0)
        editButton.layoutParams = editButtonParams

        val deleteButton = Button(this)
        deleteButton.text = "Delete"
        deleteButton.isAllCaps = false

        val deleteButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        deleteButtonParams.setMargins(dp(6), 0, 0, 0)
        deleteButton.layoutParams = deleteButtonParams

        editButton.setOnClickListener {
            editingCategory = category
            edtCategoryName.setText(category.name)
            edtCategoryLimit.setText(category.monthlyLimit.toString())
            btnSaveCategory.text = "Update Category"
        }

        deleteButton.setOnClickListener {
            deleteCategory(category)
        }

        buttonRow.addView(editButton)
        buttonRow.addView(deleteButton)

        bubble.addView(title)
        bubble.addView(limit)
        bubble.addView(buttonRow)

        categoryListContainer.addView(bubble)
    }

    private fun deleteCategory(category: FirebaseCategory) {
        repository.deleteCategory(
            uid = userUid,
            categoryId = category.id,
            onSuccess = {
                if (editingCategory?.id == category.id) {
                    clearCategoryForm()
                }

                Toast.makeText(
                    this,
                    "Category deleted.",
                    Toast.LENGTH_SHORT
                ).show()

                loadCategories()
            },
            onError = { errorMessage ->
                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun clearCategoryForm() {
        editingCategory = null
        edtCategoryName.text.clear()
        edtCategoryLimit.text.clear()
        btnSaveCategory.text = "Save Category"
    }

    private fun openLoginPage() {
        repository.logout()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "R%.2f", amount)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}