package com.example.frydayapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


import io.github.jan.supabase.postgrest.from
import com.example.frydayapp.SupabaseClientProvider
import com.example.frydayapp.Customer
import com.example.frydayapp.R

class ProfileActivity : AppCompatActivity() {

    // Views
    private lateinit var btnLogout: ImageButton
    private lateinit var profileImage: CircleImageView
    private lateinit var btnEditImage: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvChangePassword: TextView
    private lateinit var switchNotification: SwitchCompat
    private lateinit var tvPhoneNumber: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvSupportPhone: TextView
    private lateinit var tvSupportEmail: TextView

    // Edit buttons
    private lateinit var btnEditName: ImageView
    private lateinit var btnEditPhone: ImageView

    // Bottom Navigation
    private lateinit var btnHome: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnStatus: ImageButton
    private lateinit var btnProfile: ImageButton

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: com.google.firebase.auth.FirebaseUser
    private val tag = "ProfileActivity"

    companion object {
        private const val PREFS_NAME = "profile_prefs"
        private const val KEY_IMAGE_URL = "profile_image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadUserData()
        loadCustomerDataFromSupabase() // โหลดข้อมูลจาก Supabase
        loadProfileImage()
        setSelectedTab()
    }

    private fun initViews() {
        btnLogout = findViewById(R.id.btnLogout)
        profileImage = findViewById(R.id.profileImage)
        btnEditImage = findViewById(R.id.btnEditImage)
        tvUserName = findViewById(R.id.tvUserName)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvChangePassword = findViewById(R.id.tvChangePassword)
        switchNotification = findViewById(R.id.switchNotification)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)
        tvEmail = findViewById(R.id.tvEmail)
        tvSupportPhone = findViewById(R.id.tvSupportPhone)
        tvSupportEmail = findViewById(R.id.tvSupportEmail)

        // ปุ่มแก้ไขชื่อและเบอร์
        btnEditName = findViewById(R.id.btnEditName)
        btnEditPhone = findViewById(R.id.btnEditPhone)

        // Bottom Navigation
        btnHome = findViewById(R.id.btnHome)
        btnMenu = findViewById(R.id.btnMenu)
        btnStatus = findViewById(R.id.btnStatus)
        btnProfile = findViewById(R.id.btnProfile)
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            logout()
        }

        btnEditImage.setOnClickListener {
            showImageEditDialog()
        }

        tvChangePassword.setOnClickListener {
            sendPasswordResetEmail()
        }

        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "Notification: $isChecked", Toast.LENGTH_SHORT).show()
        }

        // แก้ไขชื่อ
        btnEditName.setOnClickListener {
            showEditNameDialog()
        }

        // แก้ไขเบอร์โทร
        btnEditPhone.setOnClickListener {
            showEditPhoneDialog()
        }

        // Bottom Navigation Click Listeners
        btnHome.setOnClickListener {
            animateButton(btnHome)
            startActivity(Intent(this, HomeActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnMenu.setOnClickListener {
            animateButton(btnMenu)
            startActivity(Intent(this, MenuActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnStatus.setOnClickListener {
            animateButton(btnStatus)
            startActivity(Intent(this, StatusActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnProfile.setOnClickListener {
            animateButton(btnProfile)
            // อยู่หน้า Profile แล้ว
        }
    }

    private fun setSelectedTab() {
        // หรือ
        btnHome.alpha = 0.6f        // จางปานกลาง
        btnMenu.alpha = 0.6f
        btnStatus.alpha = 0.6f
        btnProfile.alpha = 1.0f     // ปกติ
    }
    private fun animateButton(button: ImageButton) {
        try {
            val anim = AnimationUtils.loadAnimation(this, R.anim.nav_scale)
            button.startAnimation(anim)
        } catch (e: Exception) {}
    }

    private fun loadUserData() {
        // แสดงอีเมลจาก Firebase
        tvEmail.text = currentUser.email ?: "ไม่ระบุ"
    }


    private fun loadCustomerDataFromSupabase() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("customer")
                        .select {
                            filter {
                                eq("cus_id", currentUser.uid)
                            }
                        }
                        .decodeList<Customer>()
                }

                if (response.isNotEmpty()) {
                    val customer = response.first()


                    val username = customer.username
                    if (!username.isNullOrEmpty()) {
                        tvUserName.text = username
                        tvProfileName.text = username
                        Log.d(tag, "Loaded username: $username")
                    } else {
                        // ถ้าไม่มี username ให้ใช้ email
                        val emailName = currentUser.email?.substringBefore("@") ?: "User"
                        tvUserName.text = emailName
                        tvProfileName.text = emailName
                    }

                    val phone = customer.tel
                    if (!phone.isNullOrEmpty()) {
                        tvPhoneNumber.text = phone
                        Log.d(tag, "Loaded phone: $phone")
                    }
                } else {
                    // ถ้าไม่มีใน Supabase ให้ใช้ข้อมูลจาก Firebase
                    val displayName = currentUser.displayName
                    if (!displayName.isNullOrEmpty()) {
                        tvUserName.text = displayName
                        tvProfileName.text = displayName
                    } else {
                        val emailName = currentUser.email?.substringBefore("@") ?: "User"
                        tvUserName.text = emailName
                        tvProfileName.text = emailName
                    }

                    val phone = currentUser.phoneNumber
                    if (!phone.isNullOrEmpty()) {
                        tvPhoneNumber.text = phone
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading customer data: ${e.message}")
                // Fallback ใช้ข้อมูลจาก Firebase
                val displayName = currentUser.displayName
                if (!displayName.isNullOrEmpty()) {
                    tvUserName.text = displayName
                    tvProfileName.text = displayName
                } else {
                    val emailName = currentUser.email?.substringBefore("@") ?: "User"
                    tvUserName.text = emailName
                    tvProfileName.text = emailName
                }
            }
        }
    }

    private fun showEditNameDialog() {
        val currentName = tvProfileName.text.toString()

        val input = EditText(this)
        input.setText(currentName)
        input.setPadding(20, 20, 20, 20)

        AlertDialog.Builder(this)
            .setTitle("Edit Name")
            .setMessage("Enter your new name:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != currentName) {
                    updateNameInFirebaseAndSupabase(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPhoneDialog() {
        val currentPhone = tvPhoneNumber.text.toString()

        val input = EditText(this)
        input.setText(currentPhone)
        input.hint = "Enter new phone number"
        input.inputType = android.text.InputType.TYPE_CLASS_PHONE
        input.setPadding(20, 20, 20, 20)

        AlertDialog.Builder(this)
            .setTitle("Edit Phone Number")
            .setMessage("Enter your new phone number:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newPhone = input.text.toString().trim()
                if (newPhone.isNotEmpty() && newPhone != currentPhone) {
                    updatePhoneInSupabase(newPhone)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateNameInFirebaseAndSupabase(newName: String) {
        // อัปเดตใน Firebase
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // อัปเดตใน Supabase
                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                SupabaseClientProvider.client
                                    .from("customer")
                                    .update(
                                        mapOf(
                                            "username" to newName
                                        )
                                    ) {
                                        filter {
                                            eq("cus_id", currentUser.uid)
                                        }
                                    }
                            }

                            runOnUiThread {
                                tvUserName.text = newName
                                tvProfileName.text = newName
                                Toast.makeText(this@ProfileActivity, "Name updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            Log.d(tag, "Name updated to: $newName in both Firebase and Supabase")

                        } catch (e: Exception) {
                            Log.e(tag, "Error updating name in Supabase: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@ProfileActivity, "Name updated in Firebase only", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to update name: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updatePhoneInSupabase(newPhone: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("customer")
                        .update(
                            mapOf(
                                "tel" to newPhone
                            )
                        ) {
                            filter {
                                eq("cus_id", currentUser.uid)
                            }
                        }
                }

                runOnUiThread {
                    tvPhoneNumber.text = newPhone
                    Toast.makeText(this@ProfileActivity, "Phone number updated successfully", Toast.LENGTH_SHORT).show()
                }
                Log.d(tag, "Phone updated to: $newPhone")

            } catch (e: Exception) {
                Log.e(tag, "Error updating phone: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Failed to update phone: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProfileImage() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val imageUrl = prefs.getString(KEY_IMAGE_URL, null)

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun saveProfileImage(url: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(KEY_IMAGE_URL, url).apply()
    }

    private fun showImageEditDialog() {
        val input = EditText(this)
        input.hint = "Enter image URL"
        input.setPadding(20, 20, 20, 20)

        AlertDialog.Builder(this)
            .setTitle("Change Profile Image")
            .setMessage("Enter URL of your profile image:")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(profileImage)

                    saveProfileImage(url)

                    Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail() {
        val email = currentUser.email

        if (email != null) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        AlertDialog.Builder(this)
                            .setTitle("Reset Password")
                            .setMessage("Password reset email has been sent to $email\n\nPlease check your email and follow the instructions.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "No email found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        auth.signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // โหลดข้อมูลใหม่ทุกครั้ง
            loadCustomerDataFromSupabase()
        }
    }
}