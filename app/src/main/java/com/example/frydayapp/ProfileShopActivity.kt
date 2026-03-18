package com.example.frydayapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class ProfileShopActivity : AppCompatActivity() {

    // Views
    private lateinit var btnLogout: ImageButton
    private lateinit var profileImage: CircleImageView
    private lateinit var btnEditImage: ImageView
    private lateinit var tvRestaurantName: TextView
    private lateinit var tvDisplayName: TextView

    private lateinit var tvPhoneNumber: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAddress: TextView

    // Edit buttons
    private lateinit var btnEditPhone: ImageView
    private lateinit var btnEditEmail: ImageView
    private lateinit var btnEditAddress: ImageView

    // Edit Card
    private lateinit var cardEdit: CardView
    private lateinit var tvEditTitle: TextView
    private lateinit var etEditValue: EditText
    private lateinit var btnSaveEdit: Button

    // Bottom Navigation (3 ปุ่ม)
    private lateinit var btnMenuShop: ImageButton
    private lateinit var btnStatusShop: ImageButton
    private lateinit var btnProfileShop: ImageButton

    private lateinit var auth: FirebaseAuth
    private var currentEditField: String = ""

    companion object {
        private const val PREFS_NAME = "restaurant_prefs"
        private const val KEY_IMAGE_URL = "restaurant_image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_shop)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupClickListeners()
        loadRestaurantData()
        loadProfileImage()
        setSelectedTab()
    }

    private fun initViews() {
        btnLogout = findViewById(R.id.btnLogout)
        profileImage = findViewById(R.id.profileImage)
        btnEditImage = findViewById(R.id.btnEditImage)
        tvRestaurantName = findViewById(R.id.tvRestaurantName)
        tvDisplayName = findViewById(R.id.tvDisplayName)

        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)
        tvEmail = findViewById(R.id.tvEmail)
        tvAddress = findViewById(R.id.tvAddress)

        // Edit buttons
        btnEditPhone = findViewById(R.id.btnEditPhone)
        btnEditEmail = findViewById(R.id.btnEditEmail)
        btnEditAddress = findViewById(R.id.btnEditAddress)

        // Edit Card
        cardEdit = findViewById(R.id.cardEdit)
        tvEditTitle = findViewById(R.id.tvEditTitle)
        etEditValue = findViewById(R.id.etEditValue)
        btnSaveEdit = findViewById(R.id.btnSaveEdit)

        // ✅ Bottom Navigation - 3 ปุ่มจาก custom_bottom_nav_shop.xml
        btnMenuShop = findViewById(R.id.btn_menu_shop)
        btnStatusShop = findViewById(R.id.btn_status_shop)
        btnProfileShop = findViewById(R.id.btn_profile_shop)
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            logout()
        }

        btnEditImage.setOnClickListener {
            showImageEditDialog()
        }

        // แก้ไขเบอร์โทร
        btnEditPhone.setOnClickListener {
            showEditCard("phone", tvPhoneNumber.text.toString())
        }

        // แก้ไขอีเมล
        btnEditEmail.setOnClickListener {
            showEditCard("email", tvEmail.text.toString())
        }

        // แก้ไขที่อยู่
        btnEditAddress.setOnClickListener {
            showEditCard("address", tvAddress.text.toString())
        }

        btnSaveEdit.setOnClickListener {
            saveEdit()
        }

        // ✅ Bottom Navigation Click Listeners (3 ปุ่ม)
        btnMenuShop.setOnClickListener {
            animateButton(btnMenuShop)
            startActivity(Intent(this, HomeShopActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnStatusShop.setOnClickListener {
            animateButton(btnStatusShop)
            startActivity(Intent(this, StatusShopActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnProfileShop.setOnClickListener {
            animateButton(btnProfileShop)
            // อยู่หน้า Profile Shop แล้ว
        }
    }

    private fun setSelectedTab() {
        // ✅ ตั้งค่า alpha: ปุ่ม Profile ชัด, ปุ่มอื่นจาง
        btnMenuShop.alpha = 0.5f
        btnStatusShop.alpha = 0.5f
        btnProfileShop.alpha = 1.0f
    }

    private fun animateButton(button: ImageButton) {
        try {
            val anim = AnimationUtils.loadAnimation(this, R.anim.nav_scale)
            button.startAnimation(anim)
        } catch (e: Exception) {}
    }

    private fun showEditCard(field: String, currentValue: String) {
        currentEditField = field
        cardEdit.visibility = View.VISIBLE

        when (field) {
            "phone" -> {
                tvEditTitle.text = "Edit Phone Number"
                etEditValue.hint = "Enter new phone number"
                etEditValue.inputType = android.text.InputType.TYPE_CLASS_PHONE
            }
            "email" -> {
                tvEditTitle.text = "Edit Email"
                etEditValue.hint = "Enter new email"
                etEditValue.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            "address" -> {
                tvEditTitle.text = "Edit Address"
                etEditValue.hint = "Enter new address"
                etEditValue.inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
        }

        etEditValue.setText(currentValue)
        etEditValue.requestFocus()
    }

    private fun saveEdit() {
        val newValue = etEditValue.text.toString().trim()
        if (newValue.isNotEmpty()) {
            when (currentEditField) {
                "phone" -> tvPhoneNumber.text = newValue
                "email" -> tvEmail.text = newValue
                "address" -> tvAddress.text = newValue
            }
            Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
        }
        cardEdit.visibility = View.GONE
    }

    private fun loadRestaurantData() {
        tvRestaurantName.text = "Fry Day"
        tvDisplayName.text = "Fry Day"
        tvPhoneNumber.text = "+66 00 000 0001"
        tvEmail.text = "fryday_support@mail.com"
        tvAddress.text = "123 Main St, Bangkok"

    }

    private fun loadProfileImage() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val imageUrl = prefs.getString(KEY_IMAGE_URL, null)

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_restaurant)
                .error(R.drawable.ic_restaurant)
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.ic_restaurant)
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
            .setMessage("Enter URL of your restaurant image:")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.ic_restaurant)
                        .error(R.drawable.ic_restaurant)
                        .into(profileImage)

                    saveProfileImage(url)

                    Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        }
    }
}