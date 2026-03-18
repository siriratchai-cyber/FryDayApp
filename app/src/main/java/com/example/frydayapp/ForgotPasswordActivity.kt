package com.example.frydayapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val supabase = SupabaseClientProvider.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etTel = findViewById<EditText>(R.id.etTel)
        val btnReset = findViewById<Button>(R.id.btnReset)

        btnReset.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val telRaw = etTel.text.toString().trim()

            // ลบทุกอย่างที่ไม่ใช่ตัวเลข
            val tel = telRaw.replace(Regex("[^0-9]"), "")

            if (email.isEmpty() || tel.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("FORGOT_DEBUG", "Email=[$email]")
            Log.d("FORGOT_DEBUG", "Tel=[$tel]")

            lifecycleScope.launch {
                try {

                    val result = supabase
                        .from("customer")
                        .select {
                            filter {
                                eq("email", email)
                                eq("tel", tel)
                            }
                        }
                        .decodeList<Customer>()

                    if (result.isNotEmpty()) {

                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        this@ForgotPasswordActivity,
                                        "Reset email sent",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@ForgotPasswordActivity,
                                        "Failed to send reset email",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                    } else {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Email or phone not match",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("FORGOT_ERROR", e.toString())
                }
            }
        }
    }
}