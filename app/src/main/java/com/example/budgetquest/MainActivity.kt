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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    //global declarations
    private lateinit var etMainName: EditText
    private lateinit var etMainPassword: EditText
    private lateinit var btnMainLogin: Button
    private lateinit var btnMainRegister: Button

    private lateinit var db: AppDatabase




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // room database
        db = AppDatabase.getDatabase(this)

        //typecasting
        etMainName = findViewById(R.id.etMainName)
        etMainPassword = findViewById(R.id.etMainPassword)
        btnMainLogin = findViewById(R.id.btnMainLogin)
        btnMainRegister = findViewById(R.id.btnMainRegister)

        btnMainLogin.setOnClickListener {
            loginUser()
        }

        btnMainRegister.setOnClickListener {
            //navigate user to the register page
            val intent = Intent(this, Register::class.java)
            startActivity(intent )
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loginUser(){
        //collect info that the user put in i.e. their username and password
        val username = etMainName.text.toString().trim()
        val password = etMainPassword.text.toString().trim()

        //validation checks
        if(username.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
            return
        }

        // checks if the entered input (name and password) match the one in the database
        lifecycleScope.launch {
            //checks if user exists with matching username and password
            val foundUser = db.userDao().loginUser(username, password)

            runOnUiThread {
                if(foundUser != null){
                    // if the user is found -> login successful
                    Toast.makeText(this@MainActivity,"Login Successful", Toast.LENGTH_SHORT).show()

                    // if login successful, navigate user to home page
                    openHomePage(foundUser.id)                } else{
                    // if the user is not found then login failed
                    Toast.makeText(this@MainActivity,"Invalid username or password. Please try again", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun openHomePage(userId: Int) {
        val intent = Intent(this, Home::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
        finish()
    }
}

