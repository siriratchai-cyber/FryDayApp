package com.example.frydayapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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

class HomeShopActivity : AppCompatActivity() {

    private lateinit var btnLogout: ImageButton
    private lateinit var btnMenuShop: ImageButton
    private lateinit var btnStatusShop: ImageButton
    private lateinit var btnProfileShop: ImageButton
    private lateinit var recyclerMain: RecyclerView

    private lateinit var auth: FirebaseAuth
    private var promotionList: List<PromotionModel> = emptyList()
    private var menuList: List<MenuItemModel> = emptyList()
    private var isLoading = false
    private val tag = "HomeShopActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_shop)

        Log.d(tag, "HomeShopActivity created")
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            Log.e(tag, "No user logged in")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupClickListeners()
        setSelectedTab()

        loadPromotionsAndMenus()
    }

    private fun initViews() {
        btnLogout = findViewById(R.id.btn_logout)
        btnMenuShop = findViewById(R.id.btn_menu_shop)
        btnStatusShop = findViewById(R.id.btn_status_shop)
        btnProfileShop = findViewById(R.id.btn_profile_shop)
        recyclerMain = findViewById(R.id.recyclerMain)
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            logout()
        }

        btnMenuShop.setOnClickListener {
            animateButton(btnMenuShop)
            // อยู่หน้า HomeShop แล้ว
        }

        btnStatusShop.setOnClickListener {
            animateButton(btnStatusShop)
            startActivity(Intent(this, StatusShopActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnProfileShop.setOnClickListener {
            animateButton(btnProfileShop)
            startActivity(Intent(this, ProfileShopActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun setSelectedTab() {
        btnMenuShop.alpha = 1.0f
        btnStatusShop.alpha = 0.5f
        btnProfileShop.alpha = 0.5f
    }

    private fun animateButton(button: ImageButton) {
        try {
            val anim = AnimationUtils.loadAnimation(this, R.anim.nav_scale)
            button.startAnimation(anim)
        } catch (e: Exception) {
            Log.e(tag, "Animation error: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        recyclerMain.layoutManager = LinearLayoutManager(this)
        recyclerMain.setHasFixedSize(true)
        recyclerMain.setItemViewCacheSize(20)
    }

    private fun loadPromotionsAndMenus() {
        if (isLoading) return
        isLoading = true

        lifecycleScope.launch {
            try {
                // โหลดโปรโมชั่น
                Log.d(tag, "Loading promotions from Supabase...")
                val promoResponse = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("promotion")
                        .select()
                        .decodeList<PromotionModel>()
                }
                promotionList = promoResponse
                Log.d(tag, "Loaded ${promotionList.size} promotions")

                // โหลดเมนู
                Log.d(tag, "Loading menus from Supabase...")
                val menuResponse = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("menu")
                        .select()
                        .decodeList<MenuItemModel>()
                }
                menuList = menuResponse
                Log.d(tag, "Loaded ${menuList.size} menus")

                // แสดงข้อมูลผ่าน Adapter
                displayData()

            } catch (e: Exception) {
                Log.e(tag, "Error loading data: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@HomeShopActivity,
                    "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    private fun displayData() {
        val sectionList = mutableListOf<Any>()

        // ✅ เพิ่ม Promotion Slider (ถ้ามีโปรโมชั่น)
        if (promotionList.isNotEmpty()) {
            sectionList.add("PromotionSlider")  // อันนี้จะเรียกใช้ TYPE_PROMOTION_SLIDER
        }

        // เมนูแนะนำ (is_popular = true)
        val recommendedItems = menuList.filter { it.is_popular == true }
        if (recommendedItems.isNotEmpty()) {
            sectionList.add("RecommendedHeader")
            sectionList.addAll(recommendedItems)
        }

        // จัดกลุ่มตามหมวดหมู่
        val grouped = menuList.groupBy { it.category ?: "อื่นๆ" }
        grouped.forEach { (category, items) ->
            sectionList.add(category)
            sectionList.addAll(items)
        }

        val adapter = HomeShopSectionAdapter(
            promotions = promotionList,
            items = sectionList,
            onEditPromotionClick = {
                EditPromotionDialog(this, promotionList) {
                    loadPromotionsAndMenus()
                }.show()
            },
            onMenuItemClick = { menuItem ->
                val intent = Intent(this, EditMenuDetailActivity::class.java).apply {
                    putExtra("menu_id", menuItem.menu_id)
                    putExtra("menu_name", menuItem.name)
                }
                startActivityForResult(intent, 1001)
            }
        )
        recyclerMain.adapter = adapter
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Log.d(tag, "Edit menu finished with OK - refreshing data")
            loadPromotionsAndMenus()
        }
    }
}