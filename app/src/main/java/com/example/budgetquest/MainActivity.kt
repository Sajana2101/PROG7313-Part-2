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

class MainActivity : AppCompatActivity() {

    private lateinit var etMainEmail: EditText
    private lateinit var etMainPassword: EditText
    private lateinit var btnMainLogin: Button
    private lateinit var btnMainRegister: Button

    private lateinit var repository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        repository = FirebaseRepository()

        etMainEmail = findViewById(R.id.etMainEmail)
        etMainPassword = findViewById(R.id.etMainPassword)
        btnMainLogin = findViewById(R.id.btnMainLogin)
        btnMainRegister = findViewById(R.id.btnMainRegister)

        btnMainLogin.setOnClickListener {
            loginUser()
        }

        btnMainRegister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
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

    private fun loginUser() {
        val email = etMainEmail.text.toString().trim()
        val password = etMainPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter both email and password.",
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

        setLoginButtonsEnabled(false)

        repository.loginUser(
            email = email,
            password = password,
            onSuccess = { uid ->
                Toast.makeText(
                    this,
                    "Login successful.",
                    Toast.LENGTH_SHORT
                ).show()

                openHomePage(uid)
            },
            onError = { errorMessage ->
                setLoginButtonsEnabled(true)

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun openHomePage(userUid: String) {
        val intent = Intent(this, Home::class.java)
        intent.putExtra("userUid", userUid)
        startActivity(intent)
        finish()
    }

    private fun setLoginButtonsEnabled(isEnabled: Boolean) {
        btnMainLogin.isEnabled = isEnabled
        btnMainRegister.isEnabled = isEnabled
    }
}