package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import io.github.jan.supabase.postgrest.from
import com.example.frydayapp.SupabaseClientProvider
import com.example.frydayapp.Customer
import com.example.frydayapp.CartManager
import com.example.frydayapp.CartItem
import com.example.frydayapp.R

class OrderSummaryActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvOrderId: TextView
    private lateinit var tvRestaurantName: TextView
    private lateinit var tvRestaurantAddress: TextView
    private lateinit var tvRestaurantPhone: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvPhoneNumber: TextView
    private lateinit var tvOrderItems: TextView
    private lateinit var tvPickupTime: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvInstructionNote: TextView

    private lateinit var auth: FirebaseAuth
    private var cartItems: List<CartItem> = emptyList()
    private var totalAmount: Double = 0.0
    private var pickupTime: String = ""
    private var orderId: String = ""
    private val tag = "OrderSummary"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_summary)

        auth = FirebaseAuth.getInstance()

        // รับข้อมูลจาก Intent
        totalAmount = intent.getDoubleExtra("total_amount", 0.0)
        pickupTime = intent.getStringExtra("pickup_time") ?: ""
        orderId = intent.getStringExtra("order_id") ?: ""

        Log.d(tag, "Received orderId: $orderId")
        Log.d(tag, "Received total: $totalAmount")
        Log.d(tag, "Received pickupTime: $pickupTime")

        initViews()
        setupClickListeners()
        loadOrderData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvOrderId = findViewById(R.id.tvOrderId)
        tvRestaurantName = findViewById(R.id.tvRestaurantName)
        tvRestaurantAddress = findViewById(R.id.tvRestaurantAddress)
        tvRestaurantPhone = findViewById(R.id.tvRestaurantPhone)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)
        tvOrderItems = findViewById(R.id.tvOrderItems)
        tvPickupTime = findViewById(R.id.tvPickupTime)
        tvTotal = findViewById(R.id.tvTotal)

    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadOrderData() {
        cartItems = CartManager.getCartItems()
        val currentUser = auth.currentUser

        tvOrderId.text = if (orderId.isNotEmpty()) {
            "Order ID: #$orderId"
        } else {
            "Order ID: #0000"
        }

        // ข้อมูลร้าน
        tvRestaurantName.text = "FryDay Restaurant"
        tvRestaurantAddress.text = "123 Main Street, Bangkok"
        tvRestaurantPhone.text = "098-105-3288"

        // โหลดข้อมูลลูกค้า
        if (currentUser != null) {
            loadCustomerDetails(currentUser.uid)
        } else {
            tvCustomerName.text = "Customer"
            tvPhoneNumber.text = ""
        }

        // แสดงรายการอาหาร
        val itemsText = cartItems.joinToString("\n") { item ->
            val baseText = "${item.name} x${item.quantity}"
            val optionsText = if (item.options.isNotEmpty()) {
                " (${item.options.joinToString { it.name }})"
            } else ""
            "$baseText$optionsText"
        }
        tvOrderItems.text = itemsText

        // แสดงเวลานัดรับ
        tvPickupTime.text = pickupTime

        // แสดงยอดรวม
        tvTotal.text = "$${String.format("%.2f", totalAmount)}"
    }

    private fun loadCustomerDetails(uid: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("customer")
                        .select {
                            filter {
                                eq("cus_id", uid)
                            }
                        }
                        .decodeList<Customer>()
                }

                if (response.isNotEmpty()) {
                    val customer = response.first()
                    tvCustomerName.text = customer.username ?: "Customer"
                    tvPhoneNumber.text = customer.tel ?: ""

                    Log.d(tag, "Loaded customer: ${customer.username}, ${customer.tel}")
                } else {
                    // Fallback ใช้ Firebase
                    val user = auth.currentUser
                    tvCustomerName.text = user?.displayName ?: user?.email?.substringBefore("@") ?: "Customer"
                    tvPhoneNumber.text = user?.phoneNumber ?: ""
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading customer: ${e.message}")
                // Fallback ใช้ Firebase
                val user = auth.currentUser
                tvCustomerName.text = user?.displayName ?: user?.email?.substringBefore("@") ?: "Customer"
                tvPhoneNumber.text = user?.phoneNumber ?: ""
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}