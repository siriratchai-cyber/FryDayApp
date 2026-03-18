package com.example.frydayapp

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Serializable
data class OrderResponse(
    val order_id: String,
    val order_date: String,
    val total: Double,
    val pay_status: Boolean,
    val cus_id: String,
    val status_id: String,
    val username: String? = null,
    val pickup_time: String? = null,
    val payment_method: String? = null
)

@Serializable
data class CustomerResponse(
    val username: String? = null,
    val tel: String? = null
)

@Serializable
data class OrderDetailResponse(
    val order_id: String,
    val menu_id: String,
    val quantity: Int,
    val line_total: Double,
    val special_instructions: String? = null
)

@Serializable
data class OrderAddonResponse(
    val order_id: String? = null,
    val menu_id: String? = null,
    val addon_id: String,
    val quantity: Int? = null
)

@Serializable
data class OrderInsert(
    val order_id: String,
    val order_date: String,
    val total: Double,
    val pay_status: Boolean,
    val cus_id: String,
    val status_id: String,
    val username: String,
    val pickup_time: String? = null,
    val payment_method: String? = null
)

@Serializable
data class OrderDetailInsert(
    val order_id: String,
    val menu_id: String,
    val quantity: Int,
    val line_total: Double,
    val special_instructions: String? = null
)

@Serializable
data class OrderAddonInsert(
    val order_id: String,
    val menu_id: String,
    val addon_id: String,
    val quantity: Int
)

object OrderRepository {

    private const val TAG = "OrderRepository"
    private val supabase = SupabaseClientProvider.client

    private val thaiTimeZone = TimeZone.getTimeZone("Asia/Bangkok")
    private val pickupDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply {
        timeZone = thaiTimeZone
    }

    private val mutex = Mutex()
    private var cachedOrders: List<Order>? = null
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION = 60000 // 60 วินาที

    private suspend fun getCachedOrders(): List<Order> {
        val now = System.currentTimeMillis()

        return mutex.withLock {
            if (cachedOrders != null && (now - lastCacheTime) < CACHE_DURATION) {
                Log.d(TAG, "📦 Using cached orders (${cachedOrders?.size} orders)")
                return@withLock cachedOrders!!
            }

            Log.d(TAG, "🔄 Loading fresh orders from database")

            try {
                val orders = getOrders()
                cachedOrders = orders
                lastCacheTime = now
                return@withLock orders
            } catch (e: Exception) {
                Log.e(TAG, "Error loading orders: ${e.message}")
                return@withLock cachedOrders ?: emptyList()
            }
        }
    }

    suspend fun insertOrder(order: Order): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inserting order: ${order.orderId}")

                val orderInsert = OrderInsert(
                    order_id = order.orderId,
                    order_date = order.orderDate,
                    total = order.total,
                    pay_status = (order.status == "completed"),
                    cus_id = order.cus_id,
                    status_id = getStatusId(order.status),
                    username = order.username,
                    pickup_time = order.pickupTime,
                    payment_method = order.paymentMethod
                )

                supabase.from("orders").insert(orderInsert)
                Log.d(TAG, "Order inserted: ${order.orderId}")

                order.items.forEach { item ->
                    val detailInsert = OrderDetailInsert(
                        order_id = order.orderId,
                        menu_id = item.menuId,
                        quantity = item.quantity,
                        line_total = item.price * item.quantity,
                        special_instructions = item.specialInstructions.ifEmpty { null }
                    )

                    supabase.from("order_detail").insert(detailInsert)
                    Log.d(TAG, "Order detail inserted: ${item.menuId} x${item.quantity}")

                    item.options.forEach { option ->
                        val addonInsert = OrderAddonInsert(
                            order_id = order.orderId,
                            menu_id = item.menuId,
                            addon_id = option.id,
                            quantity = 1
                        )

                        supabase.from("order_addon").insert(addonInsert)
                        Log.d(TAG, "Order addon inserted: ${option.id}")
                    }
                }

                Log.d(TAG, "Order inserted successfully: ${order.orderId}")
                true

            } catch (e: Exception) {
                Log.e(TAG, "Error inserting order: ${e.message}")
                false
            }
        }
    }

    suspend fun getOrders(): List<Order> {
        return withContext(Dispatchers.IO) {
            try {
                val ordersResponse = supabase.from("orders")
                    .select(Columns.list("order_id, order_date, total, pay_status, cus_id, status_id, username, pickup_time, payment_method"))
                    .decodeList<OrderResponse>()

                Log.d(TAG, "Fetched ${ordersResponse.size} orders from Supabase")
                convertToOrders(ordersResponse)

            } catch (e: Exception) {
                Log.e(TAG, "Error getting orders: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val statusId = when (newStatus) {
                    "confirmed" -> "S002"
                    "rejected" -> "S005"
                    "preparing" -> "S003"
                    "completed" -> "S004"
                    else -> "S001"
                }

                val updates = mapOf("status_id" to statusId)
                supabase.from("orders").update(updates) {
                    filter { eq("order_id", orderId) }
                }

                Log.d(TAG, "Order status updated: $orderId -> $newStatus")
                true

            } catch (e: Exception) {
                Log.e(TAG, "Error updating order status: ${e.message}")
                false
            }
        }
    }

    private fun getStatusId(status: String): String {
        return when (status) {
            "pending" -> "S001"
            "confirmed" -> "S002"
            "preparing" -> "S003"
            "completed" -> "S004"
            "rejected" -> "S005"
            else -> "S001"
        }
    }

    private suspend fun convertToOrders(ordersResponse: List<OrderResponse>): List<Order> {
        val orders = mutableListOf<Order>()

        ordersResponse.forEach { orderResp ->
            var customerName = orderResp.username ?: "Unknown"
            var customerPhone = ""

            try {
                val customerResponse = supabase.from("customer")
                    .select(Columns.list("username, tel")) {
                        filter { eq("cus_id", orderResp.cus_id) }
                    }
                    .decodeList<CustomerResponse>()

                if (customerResponse.isNotEmpty()) {
                    val customer = customerResponse.first()
                    customerName = customer.username ?: orderResp.username ?: "Unknown"
                    customerPhone = customer.tel ?: ""
                }
            } catch (_: Exception) { }

            val detailsResponse = supabase.from("order_detail")
                .select(Columns.list("order_id, menu_id, quantity, line_total, special_instructions")) {
                    filter { eq("order_id", orderResp.order_id) }
                }
                .decodeList<OrderDetailResponse>()

            val cartItems = detailsResponse.map { detail ->
                val addonsResponse = try {
                    supabase.from("order_addon")
                        .select(Columns.list("order_id, menu_id, addon_id, quantity")) {
                            filter {
                                and {
                                    eq("order_id", orderResp.order_id)
                                    eq("menu_id", detail.menu_id)
                                }
                            }
                        }
                        .decodeList<OrderAddonResponse>()
                } catch (_: Exception) {
                    emptyList()
                }

                val options = addonsResponse.mapNotNull { addon ->
                    if (addon.addon_id.isNotEmpty()) {
                        var addonName = "Addon"
                        try {
                            val addonInfo = supabase.from("addons")
                                .select(Columns.list("name")) {
                                    filter { eq("addon_id", addon.addon_id) }
                                }
                                .decodeList<Map<String, String>>()

                            if (addonInfo.isNotEmpty()) {
                                addonName = addonInfo[0]["name"] ?: "Addon"
                            }
                        } catch (_: Exception) { }

                        Option(
                            id = addon.addon_id,
                            name = addonName,
                            price = 0.0
                        )
                    } else {
                        null
                    }
                }

                var menuName = "Menu Item"
                try {
                    val menuResponse = supabase.from("menu")
                        .select(Columns.list("name")) {
                            filter { eq("menu_id", detail.menu_id) }
                        }
                        .decodeList<Map<String, String>>()

                    if (menuResponse.isNotEmpty()) {
                        menuName = menuResponse[0]["name"] ?: "Menu Item"
                    }
                } catch (_: Exception) { }

                CartItem(
                    menuId = detail.menu_id,
                    name = menuName,
                    price = if (detail.quantity > 0) detail.line_total / detail.quantity else 0.0,
                    quantity = detail.quantity,
                    options = options,
                    specialInstructions = detail.special_instructions ?: "",
                    imageUrl = null
                )
            }

            val status = when (orderResp.status_id) {
                "S001" -> "pending"
                "S002" -> "confirmed"
                "S003" -> "preparing"
                "S004" -> "completed"
                "S005" -> "rejected"
                else -> "pending"
            }

            var pickupTimestamp = 0L
            val pickupTimeStr = orderResp.pickup_time ?: ""

            try {
                if (pickupTimeStr.isNotEmpty()) {
                    val pickupDate = pickupDateFormat.parse(pickupTimeStr)
                    pickupTimestamp = pickupDate?.time ?: 0L
                }
            } catch (_: Exception) { }

            val order = Order(
                orderId = orderResp.order_id,
                cus_id = orderResp.cus_id,
                username = customerName,
                phoneNumber = customerPhone,
                items = cartItems,
                total = orderResp.total,
                orderDate = orderResp.order_date,
                orderTime = 0,
                pickupTime = pickupTimeStr,
                pickupTimestamp = pickupTimestamp,
                status = status,
                paymentMethod = orderResp.payment_method ?: "",
                restaurantName = "FryDay Restaurant",
                restaurantAddress = "123 Main Street, Bangkok",
                restaurantPhone = "098-105-3288",
                paymentTime = 0
            )

            orders.add(order)
        }
        return orders
    }

    /**
     * ✅ NEW: ดึงออเดอร์เดียวตาม ID (เร็วมาก)
     */
    suspend fun getOrderById(orderId: String): Order? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching order by ID: $orderId")

                val orderResponse = supabase.from("orders")
                    .select(Columns.list("order_id, order_date, total, pay_status, cus_id, status_id, username, pickup_time, payment_method")) {
                        filter { eq("order_id", orderId) }
                    }
                    .decodeList<OrderResponse>()
                    .firstOrNull()

                if (orderResponse == null) {
                    Log.e(TAG, "Order not found: $orderId")
                    return@withContext null
                }

                var customerName = orderResponse.username ?: "Unknown"
                var customerPhone = ""

                try {
                    val customerResponse = supabase.from("customer")
                        .select(Columns.list("username, tel")) {
                            filter { eq("cus_id", orderResponse.cus_id) }
                        }
                        .decodeList<CustomerResponse>()

                    if (customerResponse.isNotEmpty()) {
                        val customer = customerResponse.first()
                        customerName = customer.username ?: orderResponse.username ?: "Unknown"
                        customerPhone = customer.tel ?: ""
                    }
                } catch (_: Exception) { }

                val detailsResponse = supabase.from("order_detail")
                    .select(Columns.list("order_id, menu_id, quantity, line_total, special_instructions")) {
                        filter { eq("order_id", orderId) }
                    }
                    .decodeList<OrderDetailResponse>()

                val cartItems = detailsResponse.map { detail ->
                    val addonsResponse = try {
                        supabase.from("order_addon")
                            .select(Columns.list("order_id, menu_id, addon_id, quantity")) {
                                filter {
                                    and {
                                        eq("order_id", orderId)
                                        eq("menu_id", detail.menu_id)
                                    }
                                }
                            }
                            .decodeList<OrderAddonResponse>()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val options = addonsResponse.mapNotNull { addon ->
                        if (addon.addon_id.isNotEmpty()) {
                            var addonName = "Addon"
                            try {
                                val addonInfo = supabase.from("addons")
                                    .select(Columns.list("name")) {
                                        filter { eq("addon_id", addon.addon_id) }
                                    }
                                    .decodeList<Map<String, String>>()

                                if (addonInfo.isNotEmpty()) {
                                    addonName = addonInfo[0]["name"] ?: "Addon"
                                }
                            } catch (_: Exception) { }

                            Option(
                                id = addon.addon_id,
                                name = addonName,
                                price = 0.0
                            )
                        } else {
                            null
                        }
                    }

                    var menuName = "Menu Item"
                    try {
                        val menuResponse = supabase.from("menu")
                            .select(Columns.list("name")) {
                                filter { eq("menu_id", detail.menu_id) }
                            }
                            .decodeList<Map<String, String>>()

                        if (menuResponse.isNotEmpty()) {
                            menuName = menuResponse[0]["name"] ?: "Menu Item"
                        }
                    } catch (_: Exception) { }

                    CartItem(
                        menuId = detail.menu_id,
                        name = menuName,
                        price = if (detail.quantity > 0) detail.line_total / detail.quantity else 0.0,
                        quantity = detail.quantity,
                        options = options,
                        specialInstructions = detail.special_instructions ?: "",
                        imageUrl = null
                    )
                }

                val status = when (orderResponse.status_id) {
                    "S001" -> "pending"
                    "S002" -> "confirmed"
                    "S003" -> "preparing"
                    "S004" -> "completed"
                    "S005" -> "rejected"
                    else -> "pending"
                }

                var pickupTimestamp = 0L
                val pickupTimeStr = orderResponse.pickup_time ?: ""

                try {
                    if (pickupTimeStr.isNotEmpty()) {
                        val pickupDate = pickupDateFormat.parse(pickupTimeStr)
                        pickupTimestamp = pickupDate?.time ?: 0L
                    }
                } catch (_: Exception) { }

                return@withContext Order(
                    orderId = orderResponse.order_id,
                    cus_id = orderResponse.cus_id,
                    username = customerName,
                    phoneNumber = customerPhone,
                    items = cartItems,
                    total = orderResponse.total,
                    orderDate = orderResponse.order_date,
                    orderTime = 0,
                    pickupTime = pickupTimeStr,
                    pickupTimestamp = pickupTimestamp,
                    status = status,
                    paymentMethod = orderResponse.payment_method ?: "",
                    restaurantName = "FryDay Restaurant",
                    restaurantAddress = "123 Main Street, Bangkok",
                    restaurantPhone = "098-105-3288",
                    paymentTime = 0
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error getting order by ID: ${e.message}")
                null
            }
        }
    }

    suspend fun getOrdersByPickupDate(pickupDate: String): List<Order> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching orders by pickup date: $pickupDate")
                val allOrders = getCachedOrders()
                val filtered = allOrders.filter { order ->
                    order.pickupTime.startsWith(pickupDate)
                }
                Log.d(TAG, "Found ${filtered.size} orders with pickup date $pickupDate")
                filtered.sortedByDescending { it.pickupTimestamp }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting orders by pickup date: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun getOrderCountByPickupMonth(year: Int, month: Int): Map<String, Int> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching order count by pickup month: $year-${month + 1}")
                val allOrders = getCachedOrders()
                val orderCountMap = mutableMapOf<String, Int>()
                val pickupDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

                allOrders.forEach { order ->
                    try {
                        val pickupDate = order.pickupTime.split(" ").firstOrNull() ?: return@forEach
                        val date = pickupDateFormat.parse(pickupDate)
                        val cal = Calendar.getInstance().apply { time = date }

                        if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                            orderCountMap[pickupDate] = orderCountMap.getOrDefault(pickupDate, 0) + 1
                        }
                    } catch (_: Exception) { }
                }

                Log.d(TAG, "Found ${orderCountMap.size} days with pickup orders in ${year}-${month + 1}")
                orderCountMap
            } catch (e: Exception) {
                Log.e(TAG, "Error getting order count by pickup month: ${e.message}")
                emptyMap()
            }
        }
    }

    suspend fun getLatestOrderNumber(): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching latest order number")
                val response = supabase.from("orders")
                    .select(Columns.list("order_id")) {
                        order("order_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(1)
                    }
                    .decodeList<Map<String, String>>()

                return@withContext if (response.isNotEmpty()) {
                    val orderId = response[0]["order_id"] ?: ""
                    if (orderId.startsWith("#")) {
                        orderId.substring(1).toIntOrNull() ?: 0
                    } else {
                        0
                    }
                } else {
                    0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting latest order: ${e.message}")
                0
            }
        }
    }
}