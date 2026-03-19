package com.example.frydayapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object OrderManager {

    private const val PREF_NAME = "orders_prefs"
    private const val KEY_ORDERS = "orders"
    private const val KEY_LAST_ORDER_NUMBER = "last_order_number"
    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()

    private var orders: MutableList<Order> = mutableListOf()
    private var isSyncing = false
    private val tag = "OrderManager"

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            loadOrders()
        }
    }

    private fun loadOrders() {
        val json = sharedPreferences?.getString(KEY_ORDERS, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Order>>() {}.type
            orders = gson.fromJson(json, type)
            Log.d(tag, "Loaded ${orders.size} orders from local")
        } else {
            orders = mutableListOf()
        }
    }

    private fun saveOrders() {
        val json = gson.toJson(orders)
        sharedPreferences?.edit()?.putString(KEY_ORDERS, json)?.apply()
    }


    fun getNextOrderNumber(): Int {
        val prefs = sharedPreferences ?: return 1
        val lastNumber = prefs.getInt(KEY_LAST_ORDER_NUMBER, 0)
        val nextNumber = lastNumber + 1
        prefs.edit().putInt(KEY_LAST_ORDER_NUMBER, nextNumber).apply()
        Log.d(tag, "Generated next order number: $nextNumber (last was $lastNumber)")
        return nextNumber
    }

    fun addOrder(order: Order) {
        orders.add(order)
        saveOrders()

        try {
            val orderNum = order.orderId.replace("#", "").toIntOrNull()
            if (orderNum != null) {
                sharedPreferences?.edit()?.putInt(KEY_LAST_ORDER_NUMBER, orderNum)?.apply()
                Log.d(tag, "Updated last order number to: $orderNum")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating last order number: ${e.message}")
        }

        // Sync กับ Supabase (background)
        CoroutineScope(Dispatchers.IO).launch {
            OrderRepository.insertOrder(order)
        }
    }

    suspend fun getOrdersFromSupabase(): List<Order> {
        return withContext(Dispatchers.IO) {
            try {
                val supabaseOrders = OrderRepository.getOrders()
                Log.d(tag, "Fetched ${supabaseOrders.size} orders from Supabase")

                // อัปเดต local
                orders.clear()
                orders.addAll(supabaseOrders)
                saveOrders()

                supabaseOrders
            } catch (e: Exception) {
                Log.e(tag, "Error fetching from Supabase: ${e.message}")
                // ถ้า error ให้ใช้ข้อมูลเก่า
                orders.toList()
            }
        }
    }

    fun getOrders(): List<Order> {
        return emptyList()
    }

    fun getOrdersByStatus(status: String): List<Order> =
        orders.filter { it.status == status }

    fun getOrdersByCustomer(cus_id: String): List<Order> =
        orders.filter { it.cus_id == cus_id }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        val index = orders.indexOfFirst { it.orderId == orderId }
        if (index != -1) {
            orders[index] = orders[index].copy(status = newStatus)
            saveOrders()

            CoroutineScope(Dispatchers.IO).launch {
                OrderRepository.updateOrderStatus(orderId, newStatus)
            }
        }
    }

    fun clearAllOrders() {
        orders.clear()
        saveOrders()
        Log.d(tag, "All orders cleared")
    }
}