package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class StatusActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerOrders: RecyclerView
    private lateinit var btnHome: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnStatus: ImageButton
    private lateinit var btnProfile: ImageButton
    private lateinit var tvEmptyState: TextView
    private lateinit var loadingState: View
    private lateinit var filterLayout: View
    private lateinit var btnThisWeek: TextView
    private lateinit var btnAll: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var orderAdapter: CustomerOrderAdapter
    private var allOrders: List<Order> = emptyList()
    private var filteredOrders: List<Order> = emptyList()
    private var currentTab = "ongoing"
    private var completedFilter = "all"
    private var isLoading = false

    //  เก็บ cache ของ order ไว้
    private var cachedOrders: List<Order>? = null
    private var lastLoadTime: Long = 0
    private val CACHE_DURATION = 30000 // 30 วินาที

    private val tag = "StatusActivity"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val mainScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        auth = FirebaseAuth.getInstance()

        initViews()
        setSelectedTab()
        setupTabLayout()
        setupRecyclerView()
        setupClickListeners()
        setupFilterListeners()


        loadOrders()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerOrders = findViewById(R.id.recyclerOrders)
        btnHome = findViewById(R.id.btnHome)
        btnMenu = findViewById(R.id.btnMenu)
        btnStatus = findViewById(R.id.btnStatus)
        btnProfile = findViewById(R.id.btnProfile)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        loadingState = findViewById(R.id.loadingState)
        filterLayout = findViewById(R.id.filterLayout)
        btnThisWeek = findViewById(R.id.btnThisWeek)
        btnAll = findViewById(R.id.btnAll)


        btnThisWeek.setBackgroundResource(R.drawable.filter_unselected_bg)
        btnAll.setBackgroundResource(R.drawable.filter_selected_bg)
        btnAll.setTextColor(ContextCompat.getColor(this, R.color.orange))
        btnThisWeek.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
    }

    private fun setSelectedTab() {
        btnHome.alpha = 0.5f
        btnMenu.alpha = 0.5f
        btnStatus.alpha = 1.0f
        btnProfile.alpha = 0.5f
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentTab = "ongoing"
                        filterLayout.visibility = View.GONE
                        filterOrders()
                    }
                    1 -> {
                        currentTab = "completed"
                        filterLayout.visibility = View.VISIBLE
                        filterOrders()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFilterListeners() {
        btnThisWeek.setOnClickListener {
            if (completedFilter != "week") {
                btnThisWeek.setBackgroundResource(R.drawable.filter_selected_bg)
                btnAll.setBackgroundResource(R.drawable.filter_unselected_bg)
                btnThisWeek.setTextColor(ContextCompat.getColor(this, R.color.orange))
                btnAll.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                completedFilter = "week"
                filterOrders()
            }
        }

        btnAll.setOnClickListener {
            if (completedFilter != "all") {
                btnAll.setBackgroundResource(R.drawable.filter_selected_bg)
                btnThisWeek.setBackgroundResource(R.drawable.filter_unselected_bg)
                btnAll.setTextColor(ContextCompat.getColor(this, R.color.orange))
                btnThisWeek.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                completedFilter = "all"
                filterOrders()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerOrders.layoutManager = LinearLayoutManager(this)
        orderAdapter = CustomerOrderAdapter(
            orders = emptyList(),
            onItemClick = { order ->
                val intent = Intent(this, CustomerOrderDetailActivity::class.java)
                intent.putExtra("order_id", order.orderId)
                startActivity(intent)
            }
        )
        recyclerOrders.adapter = orderAdapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

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
        }

        btnProfile.setOnClickListener {
            animateButton(btnProfile)
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun animateButton(button: ImageButton) {
        try {
            val anim = AnimationUtils.loadAnimation(this, R.anim.nav_scale)
            button.startAnimation(anim)
        } catch (_: Exception) {}
    }


    private fun loadOrders() {
        val currentUserId = auth.currentUser?.uid ?: return


        if (shouldUseCache()) {
            cachedOrders?.let {
                allOrders = it
                filterOrders()
                return
            }
        }

        if (isLoading) return
        isLoading = true

        showLoading(true)

        mainScope.launch {
            try {

                val ordersDeferred = async(Dispatchers.IO) {
                    OrderRepository.getOrders()
                }

                val orders = ordersDeferred.await()


                allOrders = orders.filter { it.cus_id == currentUserId }
                    .sortedByDescending { it.orderTime }


                cachedOrders = allOrders
                lastLoadTime = System.currentTimeMillis()

                filterOrders()
                Log.d(tag, "Loaded ${allOrders.size} orders")

            } catch (e: Exception) {
                Log.e(tag, "Error loading orders: ${e.message}")


                if (cachedOrders != null) {
                    allOrders = cachedOrders!!
                    filterOrders()
                } else {
                    Toast.makeText(this@StatusActivity, "Failed to load orders", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }


    private fun shouldUseCache(): Boolean {
        return cachedOrders != null &&
                (System.currentTimeMillis() - lastLoadTime) < CACHE_DURATION
    }

    private fun filterOrders() {
        Log.d(tag, "=== FILTERING ORDERS ===")
        Log.d(tag, "currentTab: $currentTab, completedFilter: $completedFilter")
        Log.d(tag, "allOrders size: ${allOrders.size}")

        val baseList = when (currentTab) {
            "ongoing" -> allOrders.filter {
                it.status == "pending" || it.status == "confirmed" || it.status == "preparing"
            }
            "completed" -> allOrders.filter { it.status == "completed" }
            else -> allOrders
        }

        Log.d(tag, "baseList size: ${baseList.size}")

        // แสดงสถานะของออเดอร์ทั้งหมด
        allOrders.groupBy { it.status }.forEach { (status, list) ->
            Log.d(tag, "Status $status: ${list.size} orders")
        }

        filteredOrders = if (currentTab == "completed" && completedFilter == "week") {
            filterOrdersThisWeek(baseList)
        } else {
            baseList
        }

        Log.d(tag, "filteredOrders size: ${filteredOrders.size}")

        // แสดงรายการออเดอร์ที่กรองได้
        filteredOrders.forEachIndexed { index, order ->
            Log.d(tag, "Filtered[$index]: ${order.orderId} - ${order.status} - ${Date(order.orderTime)}")
        }


        orderAdapter.updateItems(filteredOrders)

        if (filteredOrders.isEmpty()) {
            Log.d(tag, "Showing empty state")
            showEmptyState(true)
        } else {
            Log.d(tag, "Showing orders")
            showEmptyState(false)
            recyclerOrders.visibility = View.VISIBLE
        }
    }

    private fun filterOrdersThisWeek(orders: List<Order>): List<Order> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"))

        // ตั้งค่าเป็นวันจันทร์ของสัปดาห์นี้ เวลา 00:00
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        // ไปวันอาทิตย์
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val weekEnd = calendar.timeInMillis

        Log.d(tag, "Week range: ${Date(weekStart)} to ${Date(weekEnd)}")


        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

        return orders.filter { order ->
            try {
                val date = dateFormat.parse(order.orderDate)
                val orderTime = date?.time ?: 0L
                val inRange = orderTime in weekStart..weekEnd
                Log.d(tag, "Order ${order.orderId}: date=${order.orderDate}, time=${Date(orderTime)}, inRange=$inRange")
                inRange
            } catch (e: Exception) {
                Log.e(tag, "Error parsing date for order ${order.orderId}: ${e.message}")
                false
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingState.visibility = View.VISIBLE
            recyclerOrders.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
        } else {
            loadingState.visibility = View.GONE
        }
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerOrders.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {

            if (!shouldUseCache()) {
                loadOrders()
            } else {

                filterOrders()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}