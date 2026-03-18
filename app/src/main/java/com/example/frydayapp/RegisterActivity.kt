package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val supabase = SupabaseClientProvider.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        // Initialize views
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etTel = findViewById<EditText>(R.id.etTel)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoToLogin = findViewById<Button>(R.id.btnGoToLogin)

        // Go to Login button
        btnGoToLogin.setOnClickListener {
            finish() // กลับไปหน้า Login
        }

        btnRegister.setOnClickListener {
            // ที่เหลือเหมือนเดิม...
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val rawTel = etTel.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // sanitize tel
            val tel = rawTel.filter { it.isDigit() }

            // Validation
            if (username.isEmpty() || email.isEmpty() || tel.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()
            ) {
                toast("Please fill all fields")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("Invalid email format")
                return@setOnClickListener
            }

            if (password.length < 6) {
                toast("Password must be at least 6 characters")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                toast("Password not match")
                return@setOnClickListener
            }

            btnRegister.isEnabled = false

            // Firebase create
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    lifecycleScope.launch {
                        try {
                            supabase.from("customer").insert(
                                mapOf(
                                    "cus_id" to uid,
                                    "username" to username,
                                    "email" to email,
                                    "tel" to tel
                                )
                            )

                            toast("Register Success")

                            // กลับไปหน้า Login
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()

                        } catch (e: Exception) {
                            auth.currentUser?.delete()
                            toast("Database Error: ${e.message}")
                            btnRegister.isEnabled = true
                        }
                    }
                }
                .addOnFailureListener {
                    toast(it.message ?: "Register Failed")
                    btnRegister.isEnabled = true
                }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}