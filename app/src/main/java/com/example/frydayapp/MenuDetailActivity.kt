package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from

class MenuDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnCart: ImageButton
    private lateinit var imgMenuDetail: ImageView
    private lateinit var tvMenuName: TextView
    private lateinit var tvMenuDescription: TextView
    private lateinit var tvMenuPrice: TextView
    private lateinit var recyclerOptions: RecyclerView
    private lateinit var tvNoOptions: TextView
    private lateinit var etSpecialInstructions: EditText
    private lateinit var btnDecrease: ImageButton
    private lateinit var btnIncrease: ImageButton
    private lateinit var tvQuantity: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnAddToCart: Button

    private lateinit var auth: FirebaseAuth
    private var menuItem: MenuItemModel? = null
    private var quantity = 1
    private var selectedOptions = mutableListOf<Option>()
    private var basePrice = 0.0
    private var addonsList: List<Option> = emptyList()

    companion object {
        private const val TAG = "MenuDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_detail)

        auth = FirebaseAuth.getInstance()

        val menuId = intent.getStringExtra("menu_id") ?: ""
        if (menuId.isEmpty()) {
            Toast.makeText(this, "Menu item not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        // ใช้ Supabase โดยตรง
        loadMenuFromSupabase(menuId)
        setupClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnCart = findViewById(R.id.btnCart)
        imgMenuDetail = findViewById(R.id.imgMenuDetail)
        tvMenuName = findViewById(R.id.tvMenuName)
        tvMenuDescription = findViewById(R.id.tvMenuDescription)
        tvMenuPrice = findViewById(R.id.tvMenuPrice)
        recyclerOptions = findViewById(R.id.recyclerOptions)
        tvNoOptions = findViewById(R.id.tvNoOptions)
        etSpecialInstructions = findViewById(R.id.etSpecialInstructions)
        btnDecrease = findViewById(R.id.btnDecrease)
        btnIncrease = findViewById(R.id.btnIncrease)
        tvQuantity = findViewById(R.id.tvQuantity)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        btnAddToCart = findViewById(R.id.btnAddToCart)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
                updateTotalPrice()
            }
        }

        btnIncrease.setOnClickListener {
            quantity++
            tvQuantity.text = quantity.toString()
            updateTotalPrice()
        }

        btnAddToCart.setOnClickListener {
            addToCart()
        }
    }

    // โหลดข้อมูลจาก Supabase โดยตรง
    private fun loadMenuFromSupabase(menuId: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading menu from Supabase: $menuId")

                val response = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("menu")
                        .select {
                            filter {
                                eq("menu_id", menuId)
                            }
                        }
                        .decodeList<MenuItemModel>()
                }

                if (response.isNotEmpty()) {
                    menuItem = response.first()

                    Log.d(TAG, "=== MENU DATA FROM SUPABASE ===")
                    Log.d(TAG, "menu_id: ${menuItem?.menu_id}")
                    Log.d(TAG, "name: ${menuItem?.name}")
                    Log.d(TAG, "details: '${menuItem?.details}'")
                    Log.d(TAG, "price: ${menuItem?.price}")

                    basePrice = menuItem?.price ?: 0.0
                    setupMenuData()
                    loadAddons()
                    updateTotalPrice()
                } else {
                    Log.e(TAG, "Menu not found in Supabase")
                    Toast.makeText(this@MenuDetailActivity,
                        "Menu not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Supabase error: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@MenuDetailActivity,
                    "Error loading menu: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadAddons() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading addons from Supabase")

                val response = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("addons")
                        .select()
                        .decodeList<AddonModel>()
                }

                Log.d(TAG, "Loaded ${response.size} addons from Supabase")

                addonsList = response.map { addon ->
                    Option(
                        id = addon.addon_id,
                        name = addon.name,
                        price = addon.price ?: 0.0
                    )
                }

                setupOptions()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading addons: ${e.message}")
                e.printStackTrace()
                addonsList = emptyList()
                setupOptions()
            }
        }
    }

    private fun setupMenuData() {
        menuItem?.let { menu ->
            tvMenuName.text = menu.name ?: "ไม่มีชื่อ"

            if (!menu.details.isNullOrEmpty()) {
                tvMenuDescription.text = menu.details
                tvMenuDescription.visibility = View.VISIBLE
                Log.d(TAG, "Showing description: ${menu.details}")
            } else {
                tvMenuDescription.text = "ไม่มีคำอธิบาย"
                tvMenuDescription.visibility = View.VISIBLE
                Log.d(TAG, "No description available")
            }

            tvMenuPrice.text = String.format("$%.2f", menu.price)

            if (!menu.image_url.isNullOrEmpty()) {
                Glide.with(this)
                    .load(menu.image_url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgMenuDetail)
            } else {
                imgMenuDetail.setImageResource(R.drawable.ic_launcher_background)
            }
        }
    }

    private fun setupOptions() {
        if (addonsList.isNotEmpty()) {
            recyclerOptions.layoutManager = LinearLayoutManager(this)
            recyclerOptions.adapter = OptionAdapter(addonsList) { option, isSelected ->
                if (isSelected) {
                    selectedOptions.add(option)
                } else {
                    selectedOptions.removeAll { it.id == option.id }
                }
                updateTotalPrice()
            }
            recyclerOptions.visibility = View.VISIBLE
            tvNoOptions.visibility = View.GONE
            Log.d(TAG, "Showing ${addonsList.size} addons")
        } else {
            recyclerOptions.visibility = View.GONE
            tvNoOptions.visibility = View.VISIBLE
            Log.d(TAG, "No addons available")
        }
    }

    private fun updateTotalPrice() {
        val optionsTotal = selectedOptions.sumOf { it.price }
        val total = (basePrice + optionsTotal) * quantity
        tvTotalPrice.text = "$${String.format("%.2f", total)}"
        Log.d(TAG, "Total price updated: $total")
    }

    private fun addToCart() {
        menuItem?.let { menu ->
            val cartItem = CartItem(
                menuId = menu.menu_id,
                name = menu.name ?: "",
                price = basePrice,
                quantity = quantity,
                options = selectedOptions,
                specialInstructions = etSpecialInstructions.text.toString(),
                imageUrl = menu.image_url
            )

            CartManager.addToCart(cartItem)
            Log.d(TAG, "Added to cart: ${cartItem.name} x$quantity")
            Toast.makeText(this, "Added to cart: ${cartItem.name} x$quantity", Toast.LENGTH_SHORT).show()
            finish()
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