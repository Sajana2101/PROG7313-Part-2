package com.example.budgetquest

import Data.Database.AppDatabase
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.User
import kotlinx.coroutines.launch

class Register : AppCompatActivity() {

    //global declarations
    private lateinit var etRegName: EditText
    private lateinit var etRegPassword: EditText
    private lateinit var etRegConfirmPassword: EditText
    private lateinit var btnRegRegister: Button
    private lateinit var btnRegSignIn: Button

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        //type casting
        etRegName = findViewById(R.id.etRegName)
        etRegPassword = findViewById(R.id.etRegPassword)
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword)
        btnRegRegister = findViewById(R.id.btnRegRegister)
        btnRegSignIn = findViewById(R.id.btnRegSignIn)

        //initialize the room database
        db = AppDatabase.getDatabase(this)

        addDefaultUser() // function to add default user to the database


        // button click event
        // when the user clicks the register button, it will activate the code within
        btnRegRegister.setOnClickListener {
            registerUser()
        }

        //button for when the users already have an account
        btnRegSignIn.setOnClickListener {
            openLoginScreen()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun registerUser() {

        //get text from input fields and remove extra spaces
        // android studio doesn't accept raw data, so we need to convert it
        val username = etRegName.text.toString().trim()
        val password = etRegPassword.text.toString().trim()
        val confirmPassword = etRegConfirmPassword.text.toString().trim()

        //validation to check if fields are empty
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return // stops function if validation fails
        }

        if (password.length < 8) {
            Toast.makeText(
                this,
                "Password must be at least 8 characters long",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!password.any { it.isUpperCase() }) {
            Toast.makeText(
                this,
                "Password must contain at least one uppercase letter",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!password.any { it.isDigit() }) {
            Toast.makeText(
                this,
                "Password must contain at least one number",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        //checks if passwords match
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        //database operation
        // lifecycleScope.launch runs code in the background
        lifecycleScope.launch {
            //checks first to see if the user already exists in the database
            val existingUser = db.userDao().getUserByUsername(username)

            if (existingUser != null) {
                // if user exists, shows message on screen
                runOnUiThread {
                    Toast.makeText(this@Register, "Username already exists", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                // if user does not exist, create a new user object
                val newUser = User(
                    username = username,
                    password = password
                )

                //insert new user into the database
                db.userDao().insertUser(newUser)

                //show success message and move to the login screen
                runOnUiThread {
                    Toast.makeText(this@Register, "Register successful", Toast.LENGTH_SHORT).show()

                    clearFields()
                    openLoginScreen()
                }
            }

        }

    }

    //Creating a function for the admin
    //ensures that there is always an admin user
    private fun addDefaultUser() {
        lifecycleScope.launch {
            //check if admin already exists
            val existingUser = db.userDao().getUserByUsername("admin")
            if (existingUser == null) {
                // if not, insert the default admin
                User(username = "admin", password = "1234")
            }
        }
    }

    private fun openLoginScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // closes the current screen so user can not go back

    }

    private fun clearFields() {

    }
}