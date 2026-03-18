package com.example.frydayapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.util.*

class EditMenuDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var imgMenuDetail: ImageView
    private lateinit var tvMenuName: TextView
    private lateinit var tvMenuDescription: TextView
    private lateinit var tvMenuPrice: TextView
    private lateinit var btnEditName: ImageView
    private lateinit var btnEditDescription: ImageView
    private lateinit var btnEditPrice: ImageView
    private lateinit var recyclerOptions: RecyclerView
    private lateinit var btnAddOption: Button
    private lateinit var btnSaveAll: Button

    private lateinit var auth: FirebaseAuth
    private var menuItem: MenuItemModel? = null
    private var menuId: String = ""
    private var originalMenuName: String = ""
    private var optionsList: MutableList<Option> = mutableListOf()
    private var hasChanges = false
    private val tag = "EditMenuDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_menu_detail)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        menuId = intent.getStringExtra("menu_id") ?: ""
        val menuName = intent.getStringExtra("menu_name") ?: "Unknown"

        Log.d(tag, "=== EditMenuDetailActivity ===")
        Log.d(tag, "Received menu_id: '$menuId'")
        Log.d(tag, "Received menu_name: '$menuName'")

        if (menuId.isEmpty()) {
            Log.e(tag, "menu_id is empty!")
            Toast.makeText(this, "Menu not found (ID is empty)", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadMenuData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        imgMenuDetail = findViewById(R.id.imgMenuDetail)
        tvMenuName = findViewById(R.id.tvMenuName)
        tvMenuDescription = findViewById(R.id.tvMenuDescription)
        tvMenuPrice = findViewById(R.id.tvMenuPrice)
        btnEditName = findViewById(R.id.btnEditName)
        btnEditDescription = findViewById(R.id.btnEditDescription)
        btnEditPrice = findViewById(R.id.btnEditPrice)
        recyclerOptions = findViewById(R.id.recyclerOptions)
        btnAddOption = findViewById(R.id.btnAddOption)
        btnSaveAll = findViewById(R.id.btnSaveAll)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnSave.setOnClickListener {
            saveMenuOnly()
        }

        btnEditName.setOnClickListener {
            showEditDialog("name", tvMenuName.text.toString())
        }

        btnEditDescription.setOnClickListener {
            showEditDialog("description", tvMenuDescription.text.toString())
        }

        btnEditPrice.setOnClickListener {
            val currentPrice = tvMenuPrice.text.toString().replace("$", "").trim()
            showEditDialog("price", currentPrice)
        }

        btnAddOption.setOnClickListener {
            showAddOptionDialog()
        }

        btnSaveAll.setOnClickListener {
            saveAllChanges()
        }
    }

    private fun loadMenuData() {
        Log.d(tag, "Loading menu data for ID: $menuId")

        lifecycleScope.launch {
            try {
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

                Log.d(tag, "Response size: ${response.size}")

                if (response.isNotEmpty()) {
                    menuItem = response.first()
                    originalMenuName = menuItem?.name ?: ""

                    Log.d(tag, "=== MENU DATA LOADED ===")
                    Log.d(tag, "menu_id: ${menuItem?.menu_id}")
                    Log.d(tag, "name: ${menuItem?.name}")
                    Log.d(tag, "details: '${menuItem?.details}'")
                    Log.d(tag, "price: ${menuItem?.price}")

                    runOnUiThread {
                        displayMenuData()
                    }
                } else {
                    Log.e(tag, "Menu not found in database")
                    Toast.makeText(this@EditMenuDetailActivity,
                        "Menu not found in database", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                Log.e(tag, "Error loading menu: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@EditMenuDetailActivity,
                    "Error loading menu: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayMenuData() {
        menuItem?.let { menu ->
            tvMenuName.text = menu.name ?: "No name"
            tvMenuDescription.text = menu.details ?: "No description"
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

            loadOptions()
        }
    }

    private fun loadOptions() {
        lifecycleScope.launch {
            try {
                Log.d(tag, "Loading all global options")

                val options = withContext(Dispatchers.IO) {
                    SupabaseHelper.loadAllOptions()
                }

                optionsList.clear()
                optionsList.addAll(options)

                Log.d(tag, "Loaded ${optionsList.size} global options")
                setupOptionsRecyclerView()

            } catch (e: Exception) {
                Log.e(tag, "Error loading options: ${e.message}")
                optionsList.clear()
                setupOptionsRecyclerView()
            }
        }
    }

    private fun setupOptionsRecyclerView() {
        recyclerOptions.layoutManager = LinearLayoutManager(this)
        recyclerOptions.adapter = EditOptionAdapter(optionsList) { option, action ->
            when (action) {
                "edit" -> showEditOptionDialog(option)
                "delete" -> showDeleteOptionDialog(option)
            }
        }
    }

    private fun showEditDialog(field: String, currentValue: String) {
        val input = EditText(this).apply {
            setText(currentValue)
            setPadding(20, 20, 20, 20)

            if (field == "price") {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
        }

        val title = when (field) {
            "name" -> "Edit Name"
            "description" -> "Edit Description"
            "price" -> "Edit Price"
            else -> "Edit"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Enter new value:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newValue = input.text.toString().trim()

                when (field) {
                    "name" -> {
                        if (newValue.isNotEmpty()) {
                            tvMenuName.text = newValue
                            hasChanges = true
                        } else {
                            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "description" -> {
                        tvMenuDescription.text = newValue
                        hasChanges = true
                    }
                    "price" -> {
                        val price = newValue.toDoubleOrNull()
                        if (price != null && price >= 0) {
                            tvMenuPrice.text = String.format("$%.2f", price)
                            hasChanges = true
                        } else {
                            Toast.makeText(this, "Please enter a valid price (>= 0)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddOptionDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_option, null)
            val etOptionName = dialogView.findViewById<EditText>(R.id.etOptionName)
            val etOptionPrice = dialogView.findViewById<EditText>(R.id.etOptionPrice)

            AlertDialog.Builder(this)
                .setTitle("Add New Option")
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    val name = etOptionName.text.toString().trim()
                    val price = etOptionPrice.text.toString().toDoubleOrNull() ?: 0.0

                    if (name.isNotEmpty()) {
                        if (optionsList.any { it.name.equals(name, ignoreCase = true) }) {
                            Toast.makeText(this, "Option name already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            addNewOption(name, price)
                        }
                    } else {
                        Toast.makeText(this, "Option name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(tag, "Error showing dialog: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditOptionDialog(option: Option) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_option, null)
            val etOptionName = dialogView.findViewById<EditText>(R.id.etOptionName)
            val etOptionPrice = dialogView.findViewById<EditText>(R.id.etOptionPrice)

            etOptionName.setText(option.name)
            etOptionPrice.setText(option.price.toString())

            AlertDialog.Builder(this)
                .setTitle("Edit Option")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val name = etOptionName.text.toString().trim()
                    val price = etOptionPrice.text.toString().toDoubleOrNull() ?: 0.0

                    if (name.isNotEmpty()) {
                        if (optionsList.any { it.id != option.id && it.name.equals(name, ignoreCase = true) }) {
                            Toast.makeText(this, "Option name already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            updateOption(option.id, name, price)
                        }
                    } else {
                        Toast.makeText(this, "Option name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(tag, "Error showing dialog: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteOptionDialog(option: Option) {
        AlertDialog.Builder(this)
            .setTitle("Delete Option")
            .setMessage("Are you sure you want to delete ${option.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteOption(option.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * สร้างรหัส option แบบ AXXX โดยดูจากเลขสูงสุดที่มี
     */
    private fun generateOptionId(): String {
        // ดึงเฉพาะรหัสที่ขึ้นต้นด้วย A และตามด้วยตัวเลข
        val existingIds = optionsList
            .map { it.id }
            .filter { it.startsWith("A") && it.length > 1 }
            .mapNotNull { it.substring(1).toIntOrNull() }

        // หาเลขสูงสุด
        val maxNumber = if (existingIds.isEmpty()) 0 else existingIds.maxOrNull() ?: 0

        // เลขถัดไป (เริ่มที่ 1)
        val nextNumber = maxNumber + 1

        // จัดรูปแบบเป็น A001, A002, ...
        return String.format("A%03d", nextNumber)
    }

    private fun addNewOption(name: String, price: Double) {
        lifecycleScope.launch {
            // ✅ สร้างรหัสแบบ A001, A002, ... จากเลขสูงสุดที่มี
            val newId = generateOptionId()

            val newOption = Option(
                id = newId,
                name = name,
                price = price
            )

            Log.d(tag, "Adding option with ID: $newId")

            val success = withContext(Dispatchers.IO) {
                SupabaseHelper.addOption(newOption)
            }

            if (success) {
                optionsList.add(newOption)
                recyclerOptions.adapter?.notifyDataSetChanged()
                hasChanges = true
                Toast.makeText(this@EditMenuDetailActivity,
                    "Option added: $name ($newId)", Toast.LENGTH_SHORT).show()
                Log.d(tag, "Added option: $name with ID: $newId")
            } else {
                Toast.makeText(this@EditMenuDetailActivity,
                    "Failed to add option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateOption(optionId: String, name: String, price: Double) {
        lifecycleScope.launch {
            val index = optionsList.indexOfFirst { it.id == optionId }
            if (index != -1) {
                val updatedOption = optionsList[index].copy(name = name, price = price)

                val success = withContext(Dispatchers.IO) {
                    SupabaseHelper.updateOption(updatedOption)
                }

                if (success) {
                    optionsList[index] = updatedOption
                    recyclerOptions.adapter?.notifyDataSetChanged()
                    hasChanges = true
                    Toast.makeText(this@EditMenuDetailActivity,
                        "Option updated: $name", Toast.LENGTH_SHORT).show()
                    Log.d(tag, "Updated option: $name")
                } else {
                    Toast.makeText(this@EditMenuDetailActivity,
                        "Failed to update option", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteOption(optionId: String) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                SupabaseHelper.deleteOption(optionId)
            }

            if (success) {
                optionsList.removeAll { it.id == optionId }
                recyclerOptions.adapter?.notifyDataSetChanged()
                hasChanges = true
                Toast.makeText(this@EditMenuDetailActivity,
                    "Option deleted", Toast.LENGTH_SHORT).show()
                Log.d(tag, "Deleted option: $optionId")
            } else {
                Toast.makeText(this@EditMenuDetailActivity,
                    "Failed to delete option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveMenuOnly() {
        val updatedName = tvMenuName.text.toString().trim()
        val updatedDesc = tvMenuDescription.text.toString().trim()
        val updatedPrice = tvMenuPrice.text.toString().replace("$", "").toDoubleOrNull() ?: 0.0

        if (updatedName.isEmpty()) {
            Toast.makeText(this, "Menu name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (updatedPrice < 0) {
            Toast.makeText(this, "Price cannot be negative", Toast.LENGTH_SHORT).show()
            return
        }

        if (updatedName != originalMenuName) {
            checkDuplicateAndSave(updatedName, updatedDesc, updatedPrice, saveOptions = false)
        } else {
            performSave(updatedName, updatedDesc, updatedPrice, saveOptions = false)
        }
    }

    private fun saveAllChanges() {
        val updatedName = tvMenuName.text.toString().trim()
        val updatedDesc = tvMenuDescription.text.toString().trim()
        val updatedPrice = tvMenuPrice.text.toString().replace("$", "").toDoubleOrNull() ?: 0.0

        if (updatedName.isEmpty()) {
            Toast.makeText(this, "Menu name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (updatedPrice < 0) {
            Toast.makeText(this, "Price cannot be negative", Toast.LENGTH_SHORT).show()
            return
        }

        if (updatedName != originalMenuName) {
            checkDuplicateAndSave(updatedName, updatedDesc, updatedPrice, saveOptions = true)
        } else {
            performSave(updatedName, updatedDesc, updatedPrice, saveOptions = true)
        }
    }

    private fun checkDuplicateAndSave(
        newName: String,
        description: String,
        price: Double,
        saveOptions: Boolean
    ) {
        lifecycleScope.launch {
            try {
                val progressDialog = AlertDialog.Builder(this@EditMenuDetailActivity)
                    .setTitle("Checking...")
                    .setMessage("Checking if menu name already exists")
                    .setCancelable(false)
                    .show()

                val existingMenus = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client
                        .from("menu")
                        .select {
                            filter {
                                eq("name", newName)
                            }
                        }
                        .decodeList<MenuItemModel>()
                }

                progressDialog.dismiss()

                if (existingMenus.isNotEmpty() && existingMenus.any { it.menu_id != menuId }) {
                    AlertDialog.Builder(this@EditMenuDetailActivity)
                        .setTitle("Duplicate Name")
                        .setMessage("A menu with name '$newName' already exists. Please use a different name.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    performSave(newName, description, price, saveOptions)
                }

            } catch (e: Exception) {
                Log.e(tag, "Error checking duplicate: ${e.message}")
                Toast.makeText(this@EditMenuDetailActivity,
                    "Error checking duplicate: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSave(
        newName: String,
        description: String,
        price: Double,
        saveOptions: Boolean
    ) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Saving...")
            .setMessage("Please wait")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            try {
                Log.d(tag, "=== SAVING TO SUPABASE VIA RETROFIT ===")
                Log.d(tag, "menu_id: $menuId")
                Log.d(tag, "name: $newName")
                Log.d(tag, "details: $description")
                Log.d(tag, "price: $price")

                val menuSuccess = withContext(Dispatchers.IO) {
                    SupabaseHelper.updateMenu(menuId, newName, description, price)
                }

                if (menuSuccess) {
                    Log.d(tag, "✅ Update successful")

                    originalMenuName = newName

                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this@EditMenuDetailActivity,
                            "Menu updated successfully!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this@EditMenuDetailActivity,
                            "Failed to update menu. Please check logs.", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "Error saving: ${e.message}")
                e.printStackTrace()

                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this@EditMenuDetailActivity,
                        "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (hasChanges) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save before exiting?")
                .setPositiveButton("Save") { _, _ ->
                    saveAllChanges()
                }
                .setNegativeButton("Discard") { _, _ ->
                    super.onBackPressed()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
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

// EditOptionAdapter
class EditOptionAdapter(
    private var options: List<Option>,
    private val onItemClick: (Option, String) -> Unit
) : RecyclerView.Adapter<EditOptionAdapter.OptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edit_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun getItemCount(): Int = options.size

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOptionName: TextView = itemView.findViewById(R.id.tvOptionName)
        private val tvOptionPrice: TextView = itemView.findViewById(R.id.tvOptionPrice)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditOption)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteOption)

        fun bind(option: Option) {
            tvOptionName.text = option.name
            tvOptionPrice.text = if (option.price > 0) "+$${option.price}" else "Free"

            btnEdit.setOnClickListener {
                onItemClick(option, "edit")
            }

            btnDelete.setOnClickListener {
                onItemClick(option, "delete")
            }
        }
    }
}