package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerMenu: RecyclerView
    private lateinit var btnCart: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnStatus: ImageButton
    private lateinit var btnProfile: ImageButton

    private lateinit var auth: FirebaseAuth
    private var promotionList: List<PromotionModel> = emptyList()
    private var menuList: List<MenuItemModel> = emptyList()

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        CartManager.init(applicationContext)
        auth = FirebaseAuth.getInstance()

        initViews()
        setSelectedTab()
        setupClickListeners()
        setupRecyclerView()

        // โหลดข้อมูลจาก Supabase
        loadPromotionsAndMenus()
    }

    private fun initViews() {
        recyclerMenu = findViewById(R.id.recyclerMenu)
        btnCart = findViewById(R.id.btnCart)
        btnHome = findViewById(R.id.btnHome)
        btnMenu = findViewById(R.id.btnMenu)
        btnStatus = findViewById(R.id.btnStatus)
        btnProfile = findViewById(R.id.btnProfile)
    }

    private fun setSelectedTab() {
        btnHome.alpha = 1.0f
        btnMenu.alpha = 0.5f
        btnStatus.alpha = 0.5f
        btnProfile.alpha = 0.5f
    }

    private fun setupClickListeners() {
        btnCart.setOnClickListener {
            animateButton(btnCart)
            startActivity(Intent(this, CartActivity::class.java))
        }

        btnHome.setOnClickListener {
            animateButton(btnHome)
        }

        btnMenu.setOnClickListener {
            animateButton(btnMenu)
            startActivity(Intent(this, MenuActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnStatus.setOnClickListener {
            animateButton(btnStatus)
            startActivity(Intent(this, StatusActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnProfile.setOnClickListener {
            animateButton(btnProfile)
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerMenu.layoutManager = LinearLayoutManager(this)
        recyclerMenu.setHasFixedSize(true)
        recyclerMenu.setItemViewCacheSize(20)
    }

    // โหลดโปรโมชั่นและเมนูจาก Supabase
    private fun loadPromotionsAndMenus() {
        lifecycleScope.launch {
            try {
                // โหลดโปรโมชั่น
                Log.d(TAG, "Loading promotions from Supabase...")
                val promoResponse = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("promotion")
                        .select()
                        .decodeList<PromotionModel>()
                }
                promotionList = promoResponse
                Log.d(TAG, "Loaded ${promotionList.size} promotions")

                // โหลดเมนู
                Log.d(TAG, "Loading menus from Supabase...")
                val menuResponse = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("menu")
                        .select()
                        .decodeList<MenuItemModel>()
                }
                menuList = menuResponse
                Log.d(TAG, "Loaded ${menuList.size} menus")

                // แสดงข้อมูล
                displayData()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@HomeActivity,
                    "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayData() {
        val sectionList = mutableListOf<Any>()

        // แสดงเมนูแนะนำ (is_popular = true)
        val recommendedItems = menuList.filter { it.is_popular == true }
        if (recommendedItems.isNotEmpty()) {
            sectionList.add("Recommended")
            sectionList.addAll(recommendedItems)
        }

        // จัดกลุ่มตามหมวดหมู่
        val grouped = menuList.groupBy { it.category ?: "อื่นๆ" }

        grouped.forEach { (category, items) ->
            sectionList.add(category)
            sectionList.addAll(items)
        }

        // สร้าง adapter
        val adapter = SectionAdapter(
            promotions = promotionList,
            items = sectionList,
            onMenuItemClick = { menuItem ->
                Log.d(TAG, "Menu clicked: ${menuItem.name}")
                val intent = Intent(this, MenuDetailActivity::class.java)
                intent.putExtra("menu_id", menuItem.menu_id)
                startActivity(intent)
            }
        )

        recyclerMenu.adapter = adapter
    }

    private fun animateButton(button: ImageButton) {
        try {
            val anim = AnimationUtils.loadAnimation(this, R.anim.nav_scale)
            button.startAnimation(anim)
        } catch (e: Exception) {
            Log.e(TAG, "Animation error: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // โหลดข้อมูลใหม่ทุกครั้งที่กลับมา
            loadPromotionsAndMenus()
        }
    }
}