package com.example.budgetquest

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.budgetquest.firebase.FirebaseRepository

class Register : AppCompatActivity() {

    private lateinit var etRegName: EditText
    private lateinit var etRegEmail: EditText
    private lateinit var etRegPassword: EditText
    private lateinit var etRegConfirmPassword: EditText
    private lateinit var btnRegRegister: Button
    private lateinit var btnRegSignIn: Button

    private lateinit var repository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        repository = FirebaseRepository()

        etRegName = findViewById(R.id.etRegName)
        etRegEmail = findViewById(R.id.etRegEmail)
        etRegPassword = findViewById(R.id.etRegPassword)
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword)
        btnRegRegister = findViewById(R.id.btnRegRegister)
        btnRegSignIn = findViewById(R.id.btnRegSignIn)

        btnRegRegister.setOnClickListener {
            registerUser()
        }

        btnRegSignIn.setOnClickListener {
            openLoginScreen()
        }

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
    }

    private fun registerUser() {
        val displayName = etRegName.text.toString().trim()
        val email = etRegEmail.text.toString().trim()
        val password = etRegPassword.text.toString()
        val confirmPassword = etRegConfirmPassword.text.toString()

        if (
            displayName.isEmpty() ||
            email.isEmpty() ||
            password.isEmpty() ||
            confirmPassword.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Please fill in all fields.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(
                this,
                "Please enter a valid email address.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (password.length < 8) {
            Toast.makeText(
                this,
                "Password must be at least 8 characters long.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!password.any { it.isUpperCase() }) {
            Toast.makeText(
                this,
                "Password must contain at least one uppercase letter.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!password.any { it.isDigit() }) {
            Toast.makeText(
                this,
                "Password must contain at least one number.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(
                this,
                "Passwords do not match.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        setRegistrationButtonsEnabled(false)

        repository.registerUser(
            displayName = displayName,
            email = email,
            password = password,
            onSuccess = {
                /*
                   Firebase signs the user in immediately after registration.
                   The rest of the app is not converted yet, so sign them out
                   and return them to the login page for the normal flow.
                 */
                repository.logout()

                Toast.makeText(
                    this,
                    "Registration successful. Please log in.",
                    Toast.LENGTH_LONG
                ).show()

                clearFields()
                openLoginScreen()
            },
            onError = { errorMessage ->
                setRegistrationButtonsEnabled(true)

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun openLoginScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun clearFields() {
        etRegName.text.clear()
        etRegEmail.text.clear()
        etRegPassword.text.clear()
        etRegConfirmPassword.text.clear()
    }

    private fun setRegistrationButtonsEnabled(isEnabled: Boolean) {
        btnRegRegister.isEnabled = isEnabled
        btnRegSignIn.isEnabled = isEnabled
    }
}