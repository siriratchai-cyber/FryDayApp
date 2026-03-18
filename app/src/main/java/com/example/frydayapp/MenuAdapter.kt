package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MenuAdapter(
    private val items: List<Any>,
    private val onMenuItemClick: (MenuItemModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_menu_category_header, parent, false)
                CategoryHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_menu_full, parent, false)
                MenuItemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryHeaderViewHolder -> {
                holder.bind(items[position] as String)
            }
            is MenuItemViewHolder -> {
                holder.bind(items[position] as MenuItemModel, onMenuItemClick)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class CategoryHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryHeader: TextView = itemView.findViewById(R.id.tvCategoryHeader)

        fun bind(category: String) {
            tvCategoryHeader.text = category
        }
    }

    class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgMenuItem: ImageView = itemView.findViewById(R.id.imgMenuItem)
        private val tvMenuName: TextView = itemView.findViewById(R.id.tvMenuName)
        private val tvMenuDescription: TextView = itemView.findViewById(R.id.tvMenuDescription)
        private val tvMenuPrice: TextView = itemView.findViewById(R.id.tvMenuPrice)

        fun bind(menu: MenuItemModel, onItemClick: (MenuItemModel) -> Unit) {
            tvMenuName.text = menu.name ?: "ไม่มีชื่อ"

            // ✅ ใช้ details (ตัวเล็ก) ตามที่กำหนดใน MenuItemModel
            if (!menu.details.isNullOrEmpty()) {
                tvMenuDescription.text = menu.details
                tvMenuDescription.visibility = View.VISIBLE
            } else {
                tvMenuDescription.visibility = View.GONE
            }

            tvMenuPrice.text = "$${menu.price}"

            Glide.with(itemView.context)
                .load(menu.image_url)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imgMenuItem)

            itemView.setOnClickListener {
                onItemClick(menu)
            }
        }
    }
}