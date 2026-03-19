package com.example.frydayapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvAmountDue: TextView
    private lateinit var tvOrderId: TextView
    private lateinit var tvViewOrderDetails: TextView
    private lateinit var btnConfirmPayment: Button
    private lateinit var tvSelectPickupTime: TextView
    private lateinit var layoutPickupTime: LinearLayout

    private lateinit var auth: FirebaseAuth
    private var totalAmount: String = "220"
    private var customerName: String = ""
    private var customerPhone: String = ""
    private var selectedPickupTime: String = ""
    private var selectedPickupTimestamp: Long = 0

    private val calendar = Calendar.getInstance()
    private val tag = "PaymentActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        auth = FirebaseAuth.getInstance()
        OrderManager.init(applicationContext)

        totalAmount = intent.getStringExtra("total_amount")?.replace("$", "")?.replace("฿", "") ?: "220"

        initViews()
        setupClickListeners()
        generateOrderId()
        setDefaultPickupTime()
        loadCustomerInfo()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvAmountDue = findViewById(R.id.tvAmountDue)
        tvOrderId = findViewById(R.id.tvOrderId)
        tvViewOrderDetails = findViewById(R.id.tvViewOrderDetails)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        tvSelectPickupTime = findViewById(R.id.tvSelectPickupTime)
        layoutPickupTime = findViewById(R.id.layoutPickupTime)

        tvAmountDue.text = totalAmount
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        tvViewOrderDetails.setOnClickListener {
            val orderId = tvOrderId.text.toString()
            // ลบ # ออกถ้าต้องการเก็บเป็นตัวเลข
            val cleanOrderId = orderId.replace("#", "")

            val total = totalAmount.toDoubleOrNull() ?: 0.0
            val intent = Intent(this, OrderSummaryActivity::class.java)
            intent.putExtra("order_id", cleanOrderId)  // ส่ง "0009"
            intent.putExtra("total_amount", total)
            intent.putExtra("pickup_time", selectedPickupTime)
            startActivity(intent)
        }

        layoutPickupTime.setOnClickListener {
            showPickupTimePicker()
        }

        btnConfirmPayment.setOnClickListener {
            confirmPayment()
        }
    }

    private fun setDefaultPickupTime() {
        calendar.add(Calendar.MINUTE, 20)
        selectedPickupTimestamp = calendar.timeInMillis

        val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        selectedPickupTime = timeFormat.format(calendar.time)


        tvSelectPickupTime.text = getString(R.string.pickup_time_format, selectedPickupTime)
    }

    private fun showPickupTimePicker() {
        val minTime = Calendar.getInstance()
        minTime.add(Calendar.MINUTE, 15)

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            TimePickerDialog(this, { _, hourOfDay, minute ->
                val selected = Calendar.getInstance()
                selected.set(year, month, dayOfMonth, hourOfDay, minute)

                if (selected.timeInMillis < minTime.timeInMillis) {
                    Toast.makeText(this, R.string.pickup_time_min_error, Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                selectedPickupTimestamp = selected.timeInMillis
                val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                selectedPickupTime = timeFormat.format(selected.time)

                tvSelectPickupTime.text = getString(R.string.pickup_time_format, selectedPickupTime)

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadCustomerInfo() {
        val user = auth.currentUser

        if (user != null) {
            customerName = user.displayName ?: ""

            if (customerName.isEmpty()) {
                loadCustomerFromSupabase(user.uid)
            } else {
                Log.d(tag, "Customer name from Firebase: $customerName")
            }

            loadCustomerPhoneFromSupabase(user.uid)
        } else {
            customerName = "Customer"
            customerPhone = ""
            Log.w(tag, "User is null, using fallback values")
        }
    }

    private fun loadCustomerFromSupabase(uid: String) {
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

                customerName = response.firstOrNull()?.username ?: "Customer"
                Log.d(tag, "Loaded customer name from Supabase: $customerName")

            } catch (e: Exception) {
                Log.e(tag, "Error loading customer name: ${e.message}")
                customerName = "Customer"
            }
        }
    }

    private fun loadCustomerPhoneFromSupabase(uid: String) {
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

                customerPhone = response.firstOrNull()?.tel ?: ""
                Log.d(tag, "Loaded customer phone from Supabase: $customerPhone")

            } catch (e: Exception) {
                Log.e(tag, "Error loading customer phone: ${e.message}")
                customerPhone = ""
            }
        }
    }

    private fun generateOrderId() {
        lifecycleScope.launch {
            try {
                val latestNumber = withContext(Dispatchers.IO) {
                    OrderRepository.getLatestOrderNumber()
                }

                val nextNumber = latestNumber + 1
                tvOrderId.text = String.format(Locale.getDefault(), "#%04d", nextNumber)

                val prefs = getSharedPreferences("orders_prefs", MODE_PRIVATE)
                prefs.edit().putInt("last_order_number", nextNumber).apply()

            } catch (e: Exception) {
                Log.e(tag, "Error: ${e.message}")
                // fallback ใช้ local
                val localNumber = OrderManager.getNextOrderNumber()
                tvOrderId.text = String.format(Locale.getDefault(), "#%04d", localNumber)
            }
        }
    }

    private fun confirmPayment() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_payment_title)
            .setMessage(getString(R.string.confirm_payment_message, totalAmount))
            .setPositiveButton(R.string.yes) { _, _ ->
                processPayment()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun processPayment() {
        Toast.makeText(this, R.string.processing_payment, Toast.LENGTH_LONG).show()

        val cartItems = CartManager.getCartItems()
        val orderId = tvOrderId.text.toString()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val currentTime = System.currentTimeMillis()

        if (selectedPickupTimestamp == 0L) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, 20)
            selectedPickupTimestamp = cal.timeInMillis
            selectedPickupTime = dateFormat.format(cal.time)
        }

        val total = totalAmount.toDoubleOrNull() ?: 0.0
        val customerId = auth.currentUser?.uid ?: ""

        val finalCustomerName = customerName.ifEmpty { "Customer" }
        val finalCustomerPhone = customerPhone.ifEmpty { "" }

        Log.d(tag, "Creating order with: name=$finalCustomerName, phone=$finalCustomerPhone")

        val newOrder = Order(
            orderId = orderId,
            cus_id = customerId,
            username = finalCustomerName,
            phoneNumber = finalCustomerPhone,
            items = cartItems,
            total = total,
            orderDate = currentDate,
            orderTime = currentTime,
            pickupTime = selectedPickupTime,
            // pickupTimestamp = selectedPickupTimestamp,
            status = "pending",
            paymentMethod = "PromptPay",
            paymentTime = currentTime,
            restaurantName = "FryDay Restaurant",
            restaurantAddress = "123 Main Street, Bangkok",
            restaurantPhone = "098-105-3288"
        )

        OrderManager.addOrder(newOrder)

        CoroutineScope(Dispatchers.IO).launch {
            OrderRepository.insertOrder(newOrder)
        }

        CartManager.clearCart()

        showSuccessDialog(orderId)
    }

    private fun showSuccessDialog(orderId: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.payment_successful_title)
            .setMessage(getString(R.string.payment_successful_message, orderId, selectedPickupTime))
            .setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            loadCustomerInfo()
        }
    }
}