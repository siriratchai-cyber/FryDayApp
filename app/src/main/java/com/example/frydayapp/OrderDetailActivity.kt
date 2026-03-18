package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvOrderId: TextView
    private lateinit var tvPickupTime: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvPhoneNumber: TextView
    private lateinit var tvOrderItems: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvSpecialInstructionsLabel: TextView
    private lateinit var tvSpecialInstructions: TextView
    private lateinit var btnPaymentConfirm: Button
    private lateinit var btnPreparing: Button
    private lateinit var btnComplete: Button
    private lateinit var progressStatus: ProgressBar
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private var currentOrder: Order? = null
    private val tag = "OrderDetail"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        auth = FirebaseAuth.getInstance()

        val orderId = intent.getStringExtra("order_id") ?: ""
        if (orderId.isEmpty()) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadOrder(orderId)
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvOrderId = findViewById(R.id.tvOrderId)
        tvPickupTime = findViewById(R.id.tvPickupTime)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)
        tvOrderItems = findViewById(R.id.tvOrderItems)
        tvTotal = findViewById(R.id.tvTotal)
        tvSpecialInstructionsLabel = findViewById(R.id.tvSpecialInstructionsLabel)
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions)
        btnPaymentConfirm = findViewById(R.id.btnPaymentConfirm)
        btnPreparing = findViewById(R.id.btnPreparing)
        btnComplete = findViewById(R.id.btnComplete)
        progressStatus = findViewById(R.id.progressStatus)
        progressBar = findViewById(R.id.progressBar)  // ✅ ต้องมีใน XML
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnPaymentConfirm.setOnClickListener {
            updateOrderStatus("confirmed")
        }

        btnPreparing.setOnClickListener {
            updateOrderStatus("preparing")
        }

        btnComplete.setOnClickListener {
            updateOrderStatus("completed")
        }
    }

    private fun loadOrder(orderId: String) {
        // แสดง loading
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // ✅ ใช้ฟังก์ชันใหม่ getOrderById
                val order = withContext(Dispatchers.IO) {
                    OrderRepository.getOrderById(orderId)
                }

                progressBar.visibility = View.GONE

                if (order != null) {
                    currentOrder = order
                    displayOrder(order)
                } else {
                    Log.e(tag, "Order not found: $orderId")
                    Toast.makeText(this@OrderDetailActivity,
                        "Order not found", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e(tag, "Error loading order: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@OrderDetailActivity,
                    "Error loading order", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayOrder(order: Order) {
        tvOrderId.text = "Order ID: ${order.orderId}"
        tvPickupTime.text = "Pickup time: ${order.pickupTime}"
        tvCustomerName.text = "Name: ${order.username}"
        tvPhoneNumber.text = "Phone number: ${order.phoneNumber}"

        // แสดงรายการอาหาร
        val itemsText = order.items.joinToString("\n\n") { item ->
            val baseText = "${item.name} x${item.quantity}"
            val optionsText = if (item.options.isNotEmpty()) {
                "\n   options: ${item.options.joinToString { it.name }}"
            } else ""

            "$baseText$optionsText"
        }
        tvOrderItems.text = itemsText

        // แสดง special instructions
        val allInstructions = order.items
            .filter { it.specialInstructions.isNotEmpty() }
            .joinToString("\n") { item ->
                "• ${item.name}: ${item.specialInstructions}"
            }

        if (allInstructions.isNotEmpty()) {
            tvSpecialInstructionsLabel.visibility = View.VISIBLE
            tvSpecialInstructions.visibility = View.VISIBLE
            tvSpecialInstructions.text = allInstructions
        } else {
            tvSpecialInstructionsLabel.visibility = View.GONE
            tvSpecialInstructions.visibility = View.GONE
        }

        tvTotal.text = "Total: $${order.total}"
        updateButtonsByStatus(order.status)
    }

    private fun updateButtonsByStatus(status: String) {
        when (status) {
            "pending" -> {
                btnPaymentConfirm.isEnabled = true
                btnPreparing.isEnabled = false
                btnComplete.isEnabled = false
                progressStatus.progress = 0
            }
            "confirmed" -> {
                btnPaymentConfirm.isEnabled = false
                btnPreparing.isEnabled = true
                btnComplete.isEnabled = false
                progressStatus.progress = 33
            }
            "preparing" -> {
                btnPaymentConfirm.isEnabled = false
                btnPreparing.isEnabled = false
                btnComplete.isEnabled = true
                progressStatus.progress = 66
            }
            "completed" -> {
                btnPaymentConfirm.isEnabled = false
                btnPreparing.isEnabled = false
                btnComplete.isEnabled = false
                progressStatus.progress = 100
            }
            "rejected" -> {
                btnPaymentConfirm.isEnabled = false
                btnPreparing.isEnabled = false
                btnComplete.isEnabled = false
                progressStatus.progress = 0
                Toast.makeText(this, "This order was rejected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateOrderStatus(newStatus: String) {
        currentOrder?.let { order ->
            // อัปเดตใน Local
            OrderManager.updateOrderStatus(order.orderId, newStatus)

            // อัปเดตใน Supabase
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        OrderRepository.updateOrderStatus(order.orderId, newStatus)
                    }
                    Toast.makeText(this@OrderDetailActivity,
                        "Order ${order.orderId} ${newStatus}", Toast.LENGTH_SHORT).show()

                    // อัปเดต UI ทันที
                    currentOrder = currentOrder?.copy(status = newStatus)
                    currentOrder?.let { displayOrder(it) }

                } catch (e: Exception) {
                    Log.e(tag, "Error updating order: ${e.message}")
                    Toast.makeText(this@OrderDetailActivity,
                        "Failed to update order", Toast.LENGTH_SHORT).show()
                }
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