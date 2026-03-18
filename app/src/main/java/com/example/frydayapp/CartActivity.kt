package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class CartActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnClearCart: ImageButton
    private lateinit var recyclerCart: RecyclerView
    private lateinit var tvRestaurantName: TextView
    private lateinit var tvRestaurantAddress: TextView
    private lateinit var tvRestaurantPhone: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnCheckout: Button

    private lateinit var auth: FirebaseAuth
    private var cartItems: List<CartItem> = emptyList()
    private lateinit var cartAdapter: CartAdapter

    companion object {
        private const val TAG = "CartActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        auth = FirebaseAuth.getInstance()

        // เริ่มต้น CartManager
        //CartManager.init(applicationContext)

        initViews()
        setupClickListeners()
        setupRecyclerView()
        loadCartData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnClearCart = findViewById(R.id.btnClearCart)
        recyclerCart = findViewById(R.id.recyclerCart)
        tvRestaurantName = findViewById(R.id.tvRestaurantName)
        tvRestaurantAddress = findViewById(R.id.tvRestaurantAddress)
        tvRestaurantPhone = findViewById(R.id.tvRestaurantPhone)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvTotal = findViewById(R.id.tvTotal)
        btnCheckout = findViewById(R.id.btnCheckout)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnClearCart.setOnClickListener {
            CartManager.clearCart()
            loadCartData() // โหลดข้อมูลใหม่ (ตะกร้าว่าง)
            Toast.makeText(this, "Cart cleared", Toast.LENGTH_SHORT).show()
        }

        btnCheckout.setOnClickListener {
            proceedToCheckout()
        }
    }

    private fun setupRecyclerView() {
        recyclerCart.layoutManager = LinearLayoutManager(this)
        cartAdapter = CartAdapter(
            items = cartItems,
            onQuantityChanged = { item, newQuantity ->
                CartManager.updateQuantity(item.menuId, newQuantity)
                loadCartData() // โหลดข้อมูลใหม่
            },
            onItemRemoved = { item ->
                CartManager.removeItem(item.menuId)
                loadCartData() // โหลดข้อมูลใหม่
                Toast.makeText(this, "Removed ${item.name} from cart", Toast.LENGTH_SHORT).show()
            },
            onItemClicked = { item ->
                val intent = Intent(this, MenuDetailActivity::class.java)
                intent.putExtra("menu_id", item.menuId)
                startActivity(intent)
            }
        )
        recyclerCart.adapter = cartAdapter
    }

    private fun loadCartData() {
        cartItems = CartManager.getCartItems()
        cartAdapter.updateItems(cartItems) // อัปเดตข้อมูลใน Adapter

        tvRestaurantName.text = "FryDay Restaurant"
        tvRestaurantAddress.text = "123 Main Street, Bangkok"
        tvRestaurantPhone.text = "+66 98 105 3288"

        updateTotals()

        // ถ้าตะกร้าว่าง ให้แสดงข้อความ
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTotals() {
        val subtotal = CartManager.getSubtotal()
        val total = CartManager.getTotal()

        tvSubtotal.text = String.format("$%.2f", subtotal)
        tvTotal.text = String.format("$%.2f", total)
    }

    private fun proceedToCheckout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // ไปหน้า Payment พร้อมส่งยอดรวม
        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra("total_amount", tvTotal.text.toString())
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // โหลดข้อมูลใหม่ทุกครั้งที่กลับมาที่ Cart
            loadCartData()
        }
    }
}