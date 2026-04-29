package com.example.budgetquest

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class FullImageActivity : AppCompatActivity() {

    private lateinit var fullImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_image)

        fullImageView = findViewById(R.id.fullImageView)

        val imageUri = intent.getStringExtra("imageUri")

        if (!imageUri.isNullOrEmpty()) {
            fullImageView.setImageURI(Uri.parse(imageUri))
        }

        // tap image to close
        fullImageView.setOnClickListener {
            finish()
        }
    }
}