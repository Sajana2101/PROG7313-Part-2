package com.example.budgetquest

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.budgetquest.firebase.FirebaseExpense
import com.example.budgetquest.firebase.FirebaseRepository
import java.util.Calendar
import java.util.Locale

class Expenses : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var spnExpCategory: Spinner
    private lateinit var edtExpAmnt: EditText
    private lateinit var edtExpD8: EditText
    private lateinit var edtStartTime: EditText
    private lateinit var edtEndTime: EditText
    private lateinit var edtExpDescrip: EditText
    private lateinit var btnPhoto: Button
    private lateinit var btnExpSave: Button

    private val categoryNames = mutableListOf<String>()

    private var selectedPhotoUri: String? = null
    private var preselectedCategory: String? = null
    private var userUid: String = ""

    /*
        Receipt photos remain stored on the current device.
        Firebase stores the URI text together with the expense record.
     */
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(
                    this,
                    "No photo selected.",
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // The selected image may still display during the current session.
            }

            selectedPhotoUri = uri.toString()
            btnPhoto.text = "Photo selected"

            Toast.makeText(
                this,
                "Photo selected. Save expense to attach it.",
                Toast.LENGTH_SHORT
            ).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expenses)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        preselectedCategory = intent.getStringExtra("preselectedCategory")

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

        spnExpCategory = findViewById(R.id.spnExpCategory)
        edtExpAmnt = findViewById(R.id.edtExpAmnt)
        edtExpD8 = findViewById(R.id.edtExpD8)
        edtStartTime = findViewById(R.id.edtStartTime)
        edtEndTime = findViewById(R.id.edtEndTime)
        edtExpDescrip = findViewById(R.id.edtExpDescrip)
        btnPhoto = findViewById(R.id.btnPhoto)
        btnExpSave = findViewById(R.id.btnExpSave)

        edtExpD8.setOnClickListener {
            showDatePicker()
        }

        edtStartTime.setOnClickListener {
            showTimePicker(edtStartTime)
        }

        edtEndTime.setOnClickListener {
            showTimePicker(edtEndTime)
        }

        btnPhoto.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        btnExpSave.setOnClickListener {
            saveExpense()
        }

        NavigationHelper.setupBottomNavigation(
            activity = this,
            userUid = userUid,
            currentPage = "AddExpense"
        )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        loadCategoriesIntoSpinner()
    }

    override fun onResume() {
        super.onResume()

        if (::repository.isInitialized && userUid.isNotBlank()) {
            loadCategoriesIntoSpinner()
        }
    }

    private fun loadCategoriesIntoSpinner() {
        repository.getCategories(
            uid = userUid,
            onSuccess = { categories ->
                categoryNames.clear()
                categoryNames.add("Select category")

                categories.forEach { category ->
                    categoryNames.add(category.name)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spnExpCategory.adapter = adapter

                preselectedCategory?.let { selectedCategory ->
                    val categoryIndex = categoryNames.indexOfFirst {
                        it.equals(selectedCategory, ignoreCase = true)
                    }

                    if (categoryIndex >= 0) {
                        spnExpCategory.setSelection(categoryIndex)
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

    private fun saveExpense() {
        val category = spnExpCategory.selectedItem?.toString().orEmpty()
        val amountText = edtExpAmnt.text.toString().trim()
        val date = edtExpD8.text.toString().trim()
        val startTime = edtStartTime.text.toString().trim()
        val endTime = edtEndTime.text.toString().trim()
        val description = edtExpDescrip.text.toString().trim()

        if (
            category.isBlank() ||
            category == "Select category" ||
            amountText.isEmpty() ||
            date.isEmpty() ||
            startTime.isEmpty() ||
            endTime.isEmpty() ||
            description.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Please fill in all required fields.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(
                this,
                "Please enter a positive amount.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val expense = FirebaseExpense(
            category = category,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            description = description,
            photoUrl = selectedPhotoUri
        )

        btnExpSave.isEnabled = false

        validateDebtPaymentAndSave(expense)
    }

    private fun validateDebtPaymentAndSave(expense: FirebaseExpense) {
        repository.getDebtByExpenseCategory(
            uid = userUid,
            categoryName = expense.category,
            onSuccess = { linkedDebt ->
                if (linkedDebt == null) {
                    saveExpenseToFirebase(expense)
                    return@getDebtByExpenseCategory
                }

                repository.getExpensesByCategory(
                    uid = userUid,
                    categoryName = linkedDebt.expenseCategory,
                    onSuccess = { paymentExpenses ->
                        val totalAlreadyPaid = paymentExpenses.sumOf { it.amount }
                        val remainingBalance = linkedDebt.totalAmount - totalAlreadyPaid

                        when {
                            remainingBalance <= 0 -> {
                                btnExpSave.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "This debt has already been fully paid.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            expense.amount > remainingBalance -> {
                                btnExpSave.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Payment cannot exceed the remaining balance of ${
                                        formatMoney(remainingBalance)
                                    }.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            else -> {
                                saveExpenseToFirebase(expense)
                            }
                        }
                    },
                    onError = { errorMessage ->
                        btnExpSave.isEnabled = true

                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onError = { errorMessage ->
                btnExpSave.isEnabled = true

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun saveExpenseToFirebase(expense: FirebaseExpense) {
        repository.saveExpense(
            uid = userUid,
            expense = expense,
            onSuccess = {
                btnExpSave.isEnabled = true

                Toast.makeText(
                    this,
                    "Expense saved successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                clearExpenseForm()
            },
            onError = { errorMessage ->
                btnExpSave.isEnabled = true

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun clearExpenseForm() {
        spnExpCategory.setSelection(0)
        edtExpAmnt.text.clear()
        edtExpD8.text.clear()
        edtStartTime.text.clear()
        edtEndTime.text.clear()
        edtExpDescrip.text.clear()

        selectedPhotoUri = null
        btnPhoto.text = "Add a Photo"
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val formattedMonth =
                    String.format(Locale.getDefault(), "%02d", month + 1)

                val formattedDay =
                    String.format(Locale.getDefault(), "%02d", day)

                edtExpD8.setText("$year-$formattedMonth-$formattedDay")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, hour, minute ->
                val selectedTime =
                    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

                targetEditText.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
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
}