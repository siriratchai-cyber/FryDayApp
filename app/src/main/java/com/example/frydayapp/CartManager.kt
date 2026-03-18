package com.example.frydayapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {

    private const val PREF_NAME = "cart_prefs"
    private const val KEY_CART = "cart_items"
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    private var cartItems: MutableList<CartItem> = mutableListOf()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadCart()
    }

    private fun loadCart() {
        val json = sharedPreferences.getString(KEY_CART, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            cartItems = gson.fromJson(json, type)
        } else {
            cartItems = mutableListOf()
        }
    }

    private fun saveCart() {
        val json = gson.toJson(cartItems)
        sharedPreferences.edit().putString(KEY_CART, json).apply()
    }

    fun addToCart(item: CartItem) {
        // ตรวจสอบว่ามีสินค้านี้ในตะกร้าหรือยัง
        val existingIndex = cartItems.indexOfFirst { it.menuId == item.menuId }

        if (existingIndex != -1) {
            // ถ้ามีแล้ว เพิ่มจำนวน
            val existingItem = cartItems[existingIndex]
            cartItems[existingIndex] = existingItem.copy(
                quantity = existingItem.quantity + item.quantity,
                options = item.options,  // อัปเดต options (ถ้ามีการเปลี่ยนแปลง)
                specialInstructions = item.specialInstructions
            )
        } else {
            // ถ้ายังไม่มี เพิ่มใหม่
            cartItems.add(item)
        }

        saveCart()
    }

    fun updateQuantity(menuId: String, newQuantity: Int) {
        val index = cartItems.indexOfFirst { it.menuId == menuId }
        if (index != -1) {
            if (newQuantity <= 0) {
                cartItems.removeAt(index)
            } else {
                cartItems[index] = cartItems[index].copy(quantity = newQuantity)
            }
            saveCart()
        }
    }

    fun removeItem(menuId: String) {
        cartItems.removeAll { it.menuId == menuId }
        saveCart()
    }

    fun clearCart() {
        cartItems.clear()
        saveCart()
    }

    fun getCartItems(): List<CartItem> = cartItems.toList()

    fun getCartSize(): Int = cartItems.sumOf { it.quantity }

    fun getSubtotal(): Double = cartItems.sumOf { it.totalPrice }

    fun getTotal(): Double {
        val subtotal = getSubtotal()
        // ✅ Tax 7% อยู่ใน subtotal แล้ว (คิดต่อรายการ)
        // ✅ ไม่มี Delivery Fee (เพราะรับหน้าร้าน)
        return subtotal
    }
}