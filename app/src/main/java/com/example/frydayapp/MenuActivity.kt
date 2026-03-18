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
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from

class MenuActivity : AppCompatActivity() {

    private lateinit var recyclerMenu: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var btnCart: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnStatus: ImageButton
    private lateinit var btnProfile: ImageButton

    private lateinit var auth: FirebaseAuth
    private var menuList: List<MenuItemModel> = emptyList()
    private var categoryList: List<String> = emptyList()
    private var menuAdapter: MenuAdapter? = null
    private val categoryPositions = mutableMapOf<String, Int>()
    private val categoryEndPositions = mutableMapOf<String, Int>()

    companion object {
        private const val TAG = "MenuActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        auth = FirebaseAuth.getInstance()

        initViews()
        setSelectedTab()
        setupClickListeners()
        setupRecyclerView()
        setupTabLayout()

        // โหลดเมนูจาก Supabase
        loadMenus()
    }

    private fun initViews() {
        recyclerMenu = findViewById(R.id.recyclerMenu)
        tabLayout = findViewById(R.id.tabLayout)
        btnCart = findViewById(R.id.btnCart)
        btnHome = findViewById(R.id.btnHome)
        btnMenu = findViewById(R.id.btnMenu)
        btnStatus = findViewById(R.id.btnStatus)
        btnProfile = findViewById(R.id.btnProfile)
    }

    private fun setSelectedTab() {
        btnHome.alpha = 0.5f
        btnMenu.alpha = 1.0f
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
            startActivity(Intent(this, HomeActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        btnMenu.setOnClickListener {
            animateButton(btnMenu)
            // อยู่หน้า Menu แล้ว
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

    private fun animateButton(button: ImageButton) {
        try {
            val anim = AnimationUtils.loadAnimation(this, R.anim.nav_scale)
            button.startAnimation(anim)
        } catch (e: Exception) {
            Log.e(TAG, "Animation error: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        recyclerMenu.layoutManager = LinearLayoutManager(this)
        recyclerMenu.setHasFixedSize(true)
        recyclerMenu.setItemViewCacheSize(20)
        recyclerMenu.isNestedScrollingEnabled = true
    }

    private fun setupTabLayout() {
        // รอให้โหลดข้อมูลก่อน
    }

    private fun setupTabLayoutAfterData() {
        tabLayout.clearOnTabSelectedListeners()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val category = categoryList.getOrNull(it.position)
                    if (category != null) {
                        val position = categoryPositions[category]
                        position?.let { pos ->
                            recyclerMenu.smoothScrollToPosition(pos)
                        }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab?.let {
                    val category = categoryList.getOrNull(it.position)
                    if (category != null) {
                        val position = categoryPositions[category]
                        position?.let { pos ->
                            recyclerMenu.smoothScrollToPosition(pos)
                        }
                    }
                }
            }
        })

        recyclerMenu.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateSelectedTab()
                }
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateSelectedTab()
            }
        })
    }

    private fun updateSelectedTab() {
        val layoutManager = recyclerMenu.layoutManager as LinearLayoutManager
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            var currentCategory = categoryList.firstOrNull() ?: return

            for (category in categoryList) {
                val startPos = categoryPositions[category] ?: continue
                val endPos = categoryEndPositions[category] ?: startPos

                if (firstVisiblePosition >= startPos && firstVisiblePosition <= endPos) {
                    currentCategory = category
                    break
                }
            }

            val categoryIndex = categoryList.indexOf(currentCategory)
            if (categoryIndex != -1 && tabLayout.selectedTabPosition != categoryIndex) {
                tabLayout.getTabAt(categoryIndex)?.select()
            }
        }
    }

    // โหลดเมนูจาก Supabase
    private fun loadMenus() {
        Log.d(TAG, "Loading menus from Supabase...")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("menu")
                        .select()
                        .decodeList<MenuItemModel>()
                }

                menuList = response
                Log.d(TAG, "Loaded ${menuList.size} menus")

                // Log แต่ละเมนู
                menuList.forEachIndexed { index, menu ->
                    Log.d(TAG, "Menu[$index]: ${menu.menu_id} - ${menu.name}")
                    Log.d(TAG, "  category: ${menu.category}")
                    Log.d(TAG, "  details: ${menu.details}")
                }

                val sections = createSections()
                displayMenus(sections)
                setupCategories()
                setupTabLayoutAfterData()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading menus: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@MenuActivity,
                    "Failed to load menu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createSections(): List<Any> {
        val sections = mutableListOf<Any>()
        categoryPositions.clear()
        categoryEndPositions.clear()

        val grouped = menuList.groupBy { it.category ?: "อื่นๆ" }
        categoryList = grouped.keys.toList()

        var currentPosition = 0

        categoryList.forEach { category ->
            categoryPositions[category] = currentPosition
            sections.add(category)
            currentPosition++

            val items = grouped[category] ?: emptyList()
            sections.addAll(items)
            currentPosition += items.size
            categoryEndPositions[category] = currentPosition - 1
        }

        Log.d(TAG, "Category positions: $categoryPositions")
        return sections
    }

    private fun displayMenus(sections: List<Any>) {
        menuAdapter = MenuAdapter(sections) { menuItem ->
            Log.d(TAG, "Menu clicked: ${menuItem.name}")
            val intent = Intent(this, MenuDetailActivity::class.java)
            intent.putExtra("menu_id", menuItem.menu_id)
            startActivity(intent)
        }
        recyclerMenu.adapter = menuAdapter
    }

    private fun setupCategories() {
        tabLayout.removeAllTabs()
        categoryList.forEach { category ->
            tabLayout.addTab(tabLayout.newTab().setText(category))
        }
        Log.d(TAG, "Setup ${categoryList.size} categories")
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // โหลดข้อมูลใหม่ทุกครั้ง
            loadMenus()
        }
    }
}