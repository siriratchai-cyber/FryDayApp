package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton  // ✅ เพิ่ม import นี้
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.graphics.Color

class SectionAdapter(
    private val promotions: List<PromotionModel>,
    private val items: List<Any>,
    private val onMenuItemClick: (MenuItemModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_PROMOTION = 0
    private val TYPE_RECOMMENDED = 1
    private val TYPE_HEADER = 2
    private val TYPE_MENU = 3

    override fun getItemViewType(position: Int): Int {
        return when {
            promotions.isNotEmpty() && position == 0 -> TYPE_PROMOTION
            else -> {
                val adjustedPos = if (promotions.isNotEmpty()) position - 1 else position
                if (adjustedPos < items.size) {
                    when {
                        items[adjustedPos] is String -> {
                            if (items[adjustedPos] == "Recommended") TYPE_RECOMMENDED else TYPE_HEADER
                        }
                        else -> TYPE_MENU
                    }
                } else {
                    TYPE_MENU
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size + if (promotions.isNotEmpty()) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_PROMOTION -> {
                val view = inflater.inflate(R.layout.item_promotion_slider, parent, false)
                PromotionSliderViewHolder(view)
            }
            TYPE_RECOMMENDED -> {
                val view = inflater.inflate(R.layout.item_category_header, parent, false)
                RecommendedHeaderViewHolder(view)
            }
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_menu_card, parent, false)
                MenuItemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PromotionSliderViewHolder -> holder.bind(promotions)
            else -> {
                val adjustedPos = if (promotions.isNotEmpty()) position - 1 else position
                if (adjustedPos < items.size) {
                    when (holder) {
                        is RecommendedHeaderViewHolder -> holder.bind(items[adjustedPos] as String)
                        is CategoryHeaderViewHolder -> holder.bind(items[adjustedPos] as String)
                        is MenuItemViewHolder -> {
                            holder.bind(
                                menu = items[adjustedPos] as MenuItemModel,
                                onItemClick = onMenuItemClick
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ ViewHolder สำหรับโปรโมชั่น (ซ่อนปุ่ม Edit)
    class PromotionSliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerPromotion: RecyclerView = itemView.findViewById(R.id.recyclerPromotion)
        private val btnEdit: ImageButton? = itemView.findViewById(R.id.btnEditPromotion)  // ✅ แก้เป็น btnEditPromotion

        fun bind(promotionList: List<PromotionModel>) {
            recyclerPromotion.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            recyclerPromotion.adapter = PromotionSliderAdapter(promotionList)

            // ✅ ซ่อนปุ่ม Edit
            btnEdit?.visibility = View.GONE
        }
    }

    // ✅ ViewHolder สำหรับ Recommended
    class RecommendedHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)

        init {
            tvCategory.text = "🔥 Recommended"
            tvCategory.setTextColor(Color.parseColor("#F28C28"))
            tvCategory.textSize = 18f
        }

        fun bind(category: String) {
            // ไม่ต้องทำอะไร
        }
    }

    class CategoryHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)

        fun bind(category: String) {
            tvCategory.text = category
        }
    }

    class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgMenu: ImageView = itemView.findViewById(R.id.imgMenu)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        fun bind(menu: MenuItemModel, onItemClick: (MenuItemModel) -> Unit) {
            tvName.text = menu.name ?: "ไม่มีชื่อ"
            tvPrice.text = "$${menu.price}"

            val imageUrl = menu.image_url
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgMenu)
            } else {
                imgMenu.setImageResource(R.drawable.ic_launcher_background)
            }

            itemView.isClickable = true
            itemView.isFocusable = true
            itemView.setOnClickListener {
                onItemClick(menu)
            }
        }
    }
}