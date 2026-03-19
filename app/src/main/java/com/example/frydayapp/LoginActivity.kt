package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val supabase = SupabaseClientProvider.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // ถ้ามี session ค้างอยู่
        auth.currentUser?.let {
            routeUser(it.uid)
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoRegister = findViewById<Button>(R.id.btnGoRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                toast("Please enter email and password")
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    result.user?.uid?.let { uid ->
                        routeUser(uid)
                    }
                }
                .addOnFailureListener {
                    toast("Login Failed: ${it.message}")
                    btnLogin.isEnabled = true
                }
        }

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { toast("Reset email sent") }
                    .addOnFailureListener { toast("Error: ${it.message}") }
            }
        }
    }

    private fun routeUser(uid: String) {
        lifecycleScope.launch {
            try {
                Log.d("ROUTE", "UID = $uid")

                val response = supabase
                    .from("restaurant")
                    .select {
                        filter { eq("res_id", uid) }
                    }

                Log.d("ROUTE", "RAW = $response")

                val restaurants = response.decodeList<Restaurant>()

                val isRestaurant = restaurants.isNotEmpty()

                Log.d("ROUTE", "isRestaurant = $isRestaurant")

                val intent = if (isRestaurant) {
                    Intent(this@LoginActivity, HomeShopActivity::class.java)
                } else {
                    Intent(this@LoginActivity, HomeActivity::class.java)
                }

                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e("CRASH", "Error = ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}