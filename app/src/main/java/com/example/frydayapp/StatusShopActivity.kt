package com.example.frydayapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class StatusShopActivity : AppCompatActivity() {

    private lateinit var recyclerOrders: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var btnLogout: ImageButton
    private lateinit var btnMenuShop: ImageButton
    private lateinit var btnStatusShop: ImageButton
    private lateinit var btnProfileShop: ImageButton
    private lateinit var btnPrevDate: Button
    private lateinit var btnNextDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvOrderCount: TextView
    private lateinit var viewDateIndicator: View
    private lateinit var layoutOpenCalendar: View
    private lateinit var tvEmptyState: TextView
    private lateinit var loadingState: View

    private lateinit var auth: FirebaseAuth
    private lateinit var orderAdapter: OrderShopAdapter
    private var allOrders: List<Order> = emptyList()
    private var currentFilter = "ongoing"
    private var isLoading = false

    private var calendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Bangkok")
    }
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Bangkok")
    }

    private var orderCountMap: Map<String, Int> = emptyMap()
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val TAG = "StatusShopActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_shop)

        auth = FirebaseAuth.getInstance()
        OrderManager.init(applicationContext)

        initViews()
        setupClickListeners()
        setupRecyclerView()
        setupTabLayout()
        setSelectedTab()

        // โหลดข้อมูลวันนี้
        updateSelectedDateText()
        loadDataForCurrentDate()
    }

    private fun initViews() {
        recyclerOrders = findViewById(R.id.recyclerOrders)
        tabLayout = findViewById(R.id.tabLayout)
        btnLogout = findViewById(R.id.btnLogout)
        btnMenuShop = findViewById(R.id.btn_menu_shop)
        btnStatusShop = findViewById(R.id.btn_status_shop)
        btnProfileShop = findViewById(R.id.btn_profile_shop)
        btnPrevDate = findViewById(R.id.btnPrevDate)
        btnNextDate = findViewById(R.id.btnNextDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvOrderCount = findViewById(R.id.tvOrderCount)
        viewDateIndicator = findViewById(R.id.viewDateIndicator)
        layoutOpenCalendar = findViewById(R.id.layoutOpenCalendar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        loadingState = findViewById(R.id.loadingState)
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener { logout() }

        btnMenuShop.setOnClickListener {
            animateButton(btnMenuShop)
            val intent = Intent(this, HomeShopActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        btnStatusShop.setOnClickListener {
            animateButton(btnStatusShop)
            // อยู่หน้าเดิม
        }

        btnProfileShop.setOnClickListener {
            animateButton(btnProfileShop)
            val intent = Intent(this, ProfileShopActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        btnPrevDate.setOnClickListener { moveToPreviousDay() }
        btnNextDate.setOnClickListener { moveToNextDay() }
        layoutOpenCalendar.setOnClickListener { showDatePickerDialog() }
    }

    private fun setSelectedTab() {
        btnMenuShop.alpha = 0.6f
        btnStatusShop.alpha = 1.0f
        btnProfileShop.alpha = 0.6f
    }

    private fun animateButton(button: ImageButton) {
        try {
            val anim = AnimationUtils.loadAnimation(this, R.anim.nav_scale)
            button.startAnimation(anim)
        } catch (_: Exception) { }
    }

    private fun setupRecyclerView() {
        recyclerOrders.layoutManager = LinearLayoutManager(this)
        setupAdapter()
    }

    private fun setupAdapter() {
        orderAdapter = OrderShopAdapter(
            orders = emptyList(),
            onItemClick = { order ->
                val intent = Intent(this, OrderDetailActivity::class.java)
                intent.putExtra("order_id", order.orderId)
                startActivity(intent)
            },
            onConfirmClick = { order -> confirmOrder(order) },
            onRejectClick = { order -> rejectOrder(order) }
        )
        recyclerOrders.adapter = orderAdapter
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { currentFilter = "ongoing"; filterOrders() }
                    1 -> { currentFilter = "completed"; filterOrders() }
                    2 -> { currentFilter = "all"; filterOrders() }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateSelectedDateText() {
        tvSelectedDate.text = dateFormat.format(calendar.time)
    }

    private fun moveToPreviousDay() {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        updateSelectedDateText()
        loadDataForCurrentDate()
    }

    private fun moveToNextDay() {
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        updateSelectedDateText()
        loadDataForCurrentDate()
    }

    private fun showDatePickerDialog() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                updateSelectedDateText()
                loadDataForCurrentDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadDataForCurrentDate() {
        if (isLoading) return
        isLoading = true

        showLoading(true)
        updateSelectedDateText()

        mainScope.launch {
            val thaiTz = TimeZone.getTimeZone("Asia/Bangkok")

            val pickupDateString = dateFormat.format(calendar.time)  // เช่น "18/03/2026"

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)

            try {
                Log.d(TAG, "Loading orders for pickup date: $pickupDateString")

                val ordersDeferred = async(Dispatchers.IO) {
                    OrderRepository.getOrdersByPickupDate(pickupDateString)
                }

                val countMapDeferred = async(Dispatchers.IO) {
                    OrderRepository.getOrderCountByPickupMonth(year, month)
                }

                val orders = ordersDeferred.await()
                orderCountMap = countMapDeferred.await()

                // DEBUG: แสดง pickup_time ของแต่ละออเดอร์
                orders.forEach {
                    Log.d(TAG, "Order ${it.orderId}: pickupTime=${it.pickupTime}, status=${it.status}")
                }

                allOrders = orders

                updateDateIndicator()
                filterOrders()

                tvOrderCount.text = getString(R.string.orders_count, orders.size)
                tvOrderCount.visibility = View.VISIBLE

                Log.d(TAG, "✅ Loaded ${orders.size} orders for pickup date $pickupDateString")
                Log.d(TAG, "📅 Pickup counts for month: $orderCountMap")

                if (orders.isEmpty()) {
                    showEmptyState(true)
                    Log.d(TAG, "No orders found for pickup date $pickupDateString")
                } else {
                    showEmptyState(false)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@StatusShopActivity,
                    "Failed to load orders: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState(true)
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }

    private fun updateDateIndicator() {
        val currentDate = dateFormat.format(calendar.time)  // รูปแบบ "dd/MM/yyyy"
        val orderCount = orderCountMap[currentDate] ?: 0

        if (orderCount > 0) {
            viewDateIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            tvSelectedDate.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            viewDateIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
            tvSelectedDate.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
        }

        Log.d(TAG, "Date indicator for $currentDate: count=$orderCount")
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingState.visibility = View.VISIBLE
            recyclerOrders.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
        } else {
            loadingState.visibility = View.GONE
            // ไม่ต้อง set recyclerOrders visibility เพราะ filterOrders จะจัดการ
        }
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerOrders.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerOrders.visibility = View.VISIBLE
        }
    }

    private fun filterOrders() {
        val filteredOrders = when (currentFilter) {
            "ongoing" -> allOrders.filter {
                it.status == "pending" || it.status == "confirmed" || it.status == "preparing"
            }
            "completed" -> allOrders.filter { it.status == "completed" }
            "all" -> allOrders
            else -> allOrders
        }

        // อัปเดต adapter โดยไม่สร้างใหม่ทุกครั้ง
        if (::orderAdapter.isInitialized) {
            // สร้าง List ใหม่แต่ใช้ Adapter เดิม
            (recyclerOrders.adapter as? OrderShopAdapter)?.let { adapter ->
                // ถ้าใช้วิธีนี้ ต้องเพิ่มฟังก์ชัน updateList ใน OrderShopAdapter
                // แต่ตอนนี้ขอใช้วิธีสร้างใหม่ไปก่อน
                updateAdapterWithOrders(filteredOrders)
            } ?: run {
                updateAdapterWithOrders(filteredOrders)
            }
        } else {
            updateAdapterWithOrders(filteredOrders)
        }

        // จัดการแสดงผลตามสถานะ
        if (filteredOrders.isEmpty()) {
            showEmptyState(true)
        } else {
            showEmptyState(false)
        }
    }

    private fun updateAdapterWithOrders(orders: List<Order>) {
        orderAdapter = OrderShopAdapter(
            orders = orders,
            onItemClick = { order ->
                val intent = Intent(this, OrderDetailActivity::class.java)
                intent.putExtra("order_id", order.orderId)
                startActivity(intent)
            },
            onConfirmClick = { order -> confirmOrder(order) },
            onRejectClick = { order -> rejectOrder(order) }
        )
        recyclerOrders.adapter = orderAdapter
    }

    private fun confirmOrder(order: Order) {
        // อัปเดตใน Local ทันที
        val updatedOrder = order.copy(status = "confirmed")

        val index = allOrders.indexOfFirst { it.orderId == order.orderId }
        if (index != -1) {
            val updatedList = allOrders.toMutableList()
            updatedList[index] = updatedOrder
            allOrders = updatedList
            filterOrders()
        }

        mainScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    OrderRepository.updateOrderStatus(order.orderId, "confirmed")
                }
                Log.d(TAG, "Order ${order.orderId} confirmed in background")

                // ไม่ต้อง loadDataForCurrentDate()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating order: ${e.message}")
                Toast.makeText(this@StatusShopActivity,
                    R.string.failed_to_confirm_order, Toast.LENGTH_SHORT).show()

                // revert ถ้า error
                val revertIndex = allOrders.indexOfFirst { it.orderId == order.orderId }
                if (revertIndex != -1) {
                    val revertList = allOrders.toMutableList()
                    revertList[revertIndex] = order
                    allOrders = revertList
                    filterOrders()
                }
            }
        }
    }

    private fun rejectOrder(order: Order) {
        // อัปเดตใน Local ทันที
        val updatedOrder = order.copy(status = "rejected")

        // หา index ของออเดอร์ใน list
        val index = allOrders.indexOfFirst { it.orderId == order.orderId }
        if (index != -1) {
            // สร้าง list ใหม่เพื่อให้ RecyclerView detect การเปลี่ยนแปลง
            val updatedList = allOrders.toMutableList()
            updatedList[index] = updatedOrder
            allOrders = updatedList

            // กรองตาม tab ปัจจุบันและอัปเดต adapter
            filterOrders()
        }

        // อัปเดตใน Supabase (เบื้องหลัง)
        mainScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    OrderRepository.updateOrderStatus(order.orderId, "rejected")
                }
                Log.d(TAG, "Order ${order.orderId} rejected in background")

                // ไม่ต้องโหลดหน้าใหม่แล้ว
                // loadDataForCurrentDate()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating order: ${e.message}")
                Toast.makeText(this@StatusShopActivity,
                    R.string.failed_to_reject_order, Toast.LENGTH_SHORT).show()

                // ถ้า error ให้ revert กลับ
                val revertIndex = allOrders.indexOfFirst { it.orderId == order.orderId }
                if (revertIndex != -1) {
                    val revertList = allOrders.toMutableList()
                    revertList[revertIndex] = order
                    allOrders = revertList
                    filterOrders()
                }
            }
        }
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, R.string.logged_out_successfully, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // โหลดข้อมูลใหม่เมื่อกลับมา
            loadDataForCurrentDate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}