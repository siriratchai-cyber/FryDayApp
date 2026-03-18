package com.example.frydayapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerOrderDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvOrderNumber: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvRestaurantAddress: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvPhoneNumber: TextView
    private lateinit var tvOrderItems: TextView
    private lateinit var tvPickupTime: TextView
    private lateinit var tvMapRestaurantAddress: TextView
    private lateinit var tvMapRestaurantPhone: TextView
    private lateinit var tvMapInstruction: TextView
    private lateinit var btnGoToGoogleMap: Button

    // Progress Bar และ Step Labels
    private lateinit var progressStatus: ProgressBar
    private lateinit var tvStep1: TextView
    private lateinit var tvStep2: TextView
    private lateinit var tvStep3: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var progressBar: ProgressBar  // ✅ เพิ่มตรงนี้

    private lateinit var auth: FirebaseAuth
    private var currentOrder: Order? = null
    private val tag = "CustomerOrderDetail"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_order_detail)

        auth = FirebaseAuth.getInstance()

        val orderId = intent.getStringExtra("order_id") ?: ""
        if (orderId.isEmpty()) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadOrder(orderId)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvOrderNumber = findViewById(R.id.tvOrderNumber)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvStatusMessage = findViewById(R.id.tvStatusMessage)
        tvRestaurantAddress = findViewById(R.id.tvRestaurantAddress)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)
        tvOrderItems = findViewById(R.id.tvOrderItems)
        tvPickupTime = findViewById(R.id.tvPickupTime)
        tvMapRestaurantAddress = findViewById(R.id.tvMapRestaurantAddress)
        tvMapRestaurantPhone = findViewById(R.id.tvMapRestaurantPhone)
        tvMapInstruction = findViewById(R.id.tvMapInstruction)
        btnGoToGoogleMap = findViewById(R.id.btnGoToGoogleMap)

        // Progress Bar และ Step Labels
        progressStatus = findViewById(R.id.progressStatus)
        tvStep1 = findViewById(R.id.tvStep1)
        tvStep2 = findViewById(R.id.tvStep2)
        tvStep3 = findViewById(R.id.tvStep3)
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus)
        progressBar = findViewById(R.id.progressBar)  // ✅ เพิ่มตรงนี้
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnGoToGoogleMap.setOnClickListener {
            openGoogleMap()
        }
    }

    private fun loadOrder(orderId: String) {
        // แสดง loading
        progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                Log.d(tag, "Loading order by ID: $orderId")

                // ✅ ใช้ฟังก์ชันใหม่ getOrderById
                val order = withContext(Dispatchers.IO) {
                    OrderRepository.getOrderById(orderId)
                }

                progressBar.visibility = android.view.View.GONE

                if (order != null) {
                    currentOrder = order
                    displayOrder(order)  // ✅ เรียกฟังก์ชันแสดงข้อมูล
                } else {
                    Log.e(tag, "Order not found: $orderId")
                    Toast.makeText(this@CustomerOrderDetailActivity,
                        "Order not found", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                progressBar.visibility = android.view.View.GONE
                Log.e(tag, "Error loading order: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@CustomerOrderDetailActivity,
                    "Error loading order", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // ✅ เพิ่มฟังก์ชันนี้
    private fun displayOrder(order: Order) {
        tvOrderNumber.text = order.orderId
        tvTotalAmount.text = "$${order.total}"

        tvStatusMessage.text = when (order.status) {
            "pending" -> "⏳ Waiting for Confirmation"
            "confirmed" -> "✅ Order Confirmed"
            "preparing" -> "👨‍🍳 The restaurant is cooking"
            "completed" -> "✨ Completed"
            "rejected" -> "❌ Order Rejected"
            else -> order.status
        }

        tvRestaurantAddress.text = order.restaurantAddress
        tvCustomerName.text = order.username
        tvPhoneNumber.text = order.phoneNumber

        val itemsText = order.items.joinToString("\n") { item ->
            val baseText = "${item.name} x${item.quantity}"
            val optionsText = if (item.options.isNotEmpty()) {
                " (${item.options.joinToString { it.name }})"
            } else ""

            val instructionsText = if (item.specialInstructions.isNotEmpty()) {
                "\n   📝 ${item.specialInstructions}"
            } else ""

            "$baseText$optionsText$instructionsText"
        }
        tvOrderItems.text = itemsText

        tvPickupTime.text = order.pickupTime

        tvMapRestaurantAddress.text = order.restaurantAddress
        tvMapRestaurantPhone.text = order.restaurantPhone
        tvMapInstruction.text = "Please provide your name and order number, or show your screen to the staff to collect your food."

        // ✅ อัปเดตสถานะ
        updateOrderStatus(order.status)
    }

    private fun updateOrderStatus(status: String) {
        Log.d(tag, "🟠 updateOrderStatus called with status: $status")

        when (status) {
            "pending" -> {
                progressStatus.progress = 0
                tvStep1.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvStep2.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvStep3.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvCurrentStatus.text = "⏳ Waiting for Confirmation"
                tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.orange))
            }
            "confirmed" -> {
                progressStatus.progress = 33
                tvStep1.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvStep2.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvStep3.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvCurrentStatus.text = "✅ Order Confirmed"
                tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.orange))
            }
            "preparing" -> {
                progressStatus.progress = 66
                tvStep1.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvStep2.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvStep3.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvCurrentStatus.text = "👨‍🍳 The restaurant is cooking"
                tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.orange))
            }
            "completed" -> {
                progressStatus.progress = 100
                tvStep1.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvStep2.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvStep3.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvCurrentStatus.text = "✨ Food is ready for pickup"
                tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.orange))
            }
            "rejected" -> {
                progressStatus.progress = 0
                tvStep1.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvStep2.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvStep3.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                tvCurrentStatus.text = "❌ Order Rejected"
                tvCurrentStatus.setTextColor(Color.RED)
            }
        }
    }

    private fun openGoogleMap() {
        currentOrder?.let { order ->
            val address = order.restaurantAddress
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")
                startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } ?: run {
            Toast.makeText(this, "Order not loaded", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            intent.getStringExtra("order_id")?.let { orderId ->
                loadOrder(orderId)
            }
        }
    }
}