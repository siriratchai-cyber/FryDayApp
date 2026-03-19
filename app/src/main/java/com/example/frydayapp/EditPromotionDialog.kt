package com.example.frydayapp

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from

class EditPromotionDialog(
    private val context: Context,
    private val promotions: List<PromotionModel>,
    private val onPromotionUpdated: () -> Unit
) {

    private val supabase = SupabaseClientProvider.client
    private val tag = "EditPromotionDialog"

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_promotion, null)
        val recyclerPromo = dialogView.findViewById<RecyclerView>(R.id.recyclerPromo)
        val btnAddPromo = dialogView.findViewById<Button>(R.id.btnAddPromo)

        recyclerPromo.layoutManager = LinearLayoutManager(context)
        recyclerPromo.adapter = PromotionEditAdapter(promotions.toMutableList()) { action, promotion ->
            when (action) {
                "edit" -> showEditPromoDialog(promotion)
                "delete" -> showDeleteConfirmDialog(promotion)
            }
        }

        btnAddPromo.setOnClickListener {
            showAddPromoDialog()
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Promotions")
            .setView(dialogView)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun showAddPromoDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_promotion_form, null)
        val etName = dialogView.findViewById<EditText>(R.id.etPromoName)
        val etImageUrl = dialogView.findViewById<EditText>(R.id.etImageUrl)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivPreview)

        etImageUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = etImageUrl.text.toString()
                if (url.isNotEmpty()) {
                    Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(ivPreview)
                }
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Add New Promotion")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val imageUrl = etImageUrl.text.toString().trim()

                if (name.isNotEmpty() && imageUrl.isNotEmpty()) {
                    addPromotion(name, imageUrl)  // 🔥 ไม่ส่ง price
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPromoDialog(promotion: PromotionModel) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_promotion_form, null)
        val etName = dialogView.findViewById<EditText>(R.id.etPromoName)
        val etImageUrl = dialogView.findViewById<EditText>(R.id.etImageUrl)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivPreview)

        etName.setText(promotion.pro_name)
        etImageUrl.setText(promotion.img_url)

        Glide.with(context)
            .load(promotion.img_url)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(ivPreview)

        etImageUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = etImageUrl.text.toString()
                if (url.isNotEmpty()) {
                    Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(ivPreview)
                }
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Promotion")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val imageUrl = etImageUrl.text.toString().trim()

                if (name.isNotEmpty() && imageUrl.isNotEmpty()) {
                    updatePromotion(promotion.pro_id, name, imageUrl)  // 🔥 ไม่ส่ง price
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmDialog(promotion: PromotionModel) {
        AlertDialog.Builder(context)
            .setTitle("Delete Promotion")
            .setMessage("Are you sure you want to delete ${promotion.pro_name}?")
            .setPositiveButton("Delete") { _, _ ->
                deletePromotion(promotion.pro_id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addPromotion(name: String, imageUrl: String) {  // 🔥 ไม่มี price
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // สร้าง pro_id ใหม่
                val newId = "PRO" + System.currentTimeMillis().toString().takeLast(6)

                val newPromo = mapOf(
                    "pro_id" to newId,
                    "pro_name" to name,
                    "img_url" to imageUrl,
                    "cate_id" to "C006"
                    // 🔥 ไม่มี price
                )

                supabase.from("promotion").insert(newPromo)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Promotion added", Toast.LENGTH_SHORT).show()
                    onPromotionUpdated()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updatePromotion(proId: String, name: String, imageUrl: String) {  // 🔥 ไม่มี price
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updates = mapOf(
                    "pro_name" to name,
                    "img_url" to imageUrl
                    // 🔥 ไม่มี price
                )

                supabase.from("promotion").update(updates) {
                    filter {
                        eq("pro_id", proId)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Promotion updated", Toast.LENGTH_SHORT).show()
                    onPromotionUpdated()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deletePromotion(proId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(tag, "Attempting to delete promotion with ID: $proId")

                supabase.from("promotion").delete {
                    filter {
                        eq("pro_id", proId)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Promotion deleted", Toast.LENGTH_SHORT).show()
                    onPromotionUpdated()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(tag, "Delete error: ${e.message}")
                }
            }
        }
    }
}