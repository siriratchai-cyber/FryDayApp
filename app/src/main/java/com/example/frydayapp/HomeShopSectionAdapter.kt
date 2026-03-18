package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HomeShopSectionAdapter(
    private val promotions: List<PromotionModel>,
    private val items: List<Any>,
    private val onEditPromotionClick: () -> Unit,
    private val onMenuItemClick: (MenuItemModel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PROMOTION_SLIDER = 0  // ✅ เปลี่ยนเป็น slider
        private const val TYPE_RECOMMENDED_HEADER = 1
        private const val TYPE_CATEGORY_HEADER = 2
        private const val TYPE_MENU = 3
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when {
            item == "PromotionSlider" -> TYPE_PROMOTION_SLIDER  // ✅ ใช้ slider
            item == "RecommendedHeader" -> TYPE_RECOMMENDED_HEADER
            item is String && !item.startsWith("Promotion") && !item.startsWith("Recommended") -> TYPE_CATEGORY_HEADER
            item is MenuItemModel -> TYPE_MENU
            else -> TYPE_MENU
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PROMOTION_SLIDER -> {
                val view = inflater.inflate(R.layout.item_promotion_slider, parent, false)
                PromotionSliderViewHolder(view)
            }
            TYPE_RECOMMENDED_HEADER -> {
                val view = inflater.inflate(R.layout.item_recommended_header, parent, false)
                RecommendedHeaderViewHolder(view)
            }
            TYPE_CATEGORY_HEADER -> {
                val view = inflater.inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_menu_shop, parent, false)
                MenuItemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PromotionSliderViewHolder -> {
                holder.bind(promotions, onEditPromotionClick, true)
            }
            is RecommendedHeaderViewHolder -> {
                holder.bind()
            }
            is CategoryHeaderViewHolder -> {
                val item = items[position] as String
                holder.bind(item)
            }
            is MenuItemViewHolder -> {
                val item = items[position] as MenuItemModel
                holder.bind(item, onMenuItemClick)
            }
        }
    }

    // ✅ ViewHolder สำหรับ Promotion Slider
    class PromotionSliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerPromotion: RecyclerView = itemView.findViewById(R.id.recyclerPromotion)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditPromotion)


        fun bind(promotionList: List<PromotionModel>, onEditClick: () -> Unit, isRestaurant: Boolean = false) {
            recyclerPromotion.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            recyclerPromotion.adapter = PromotionSliderAdapter(promotionList)

            // ✅ ถ้าเป็นร้านค้า ให้แสดงปุ่ม Edit, ถ้าเป็นลูกค้าให้ซ่อน
            if (isRestaurant) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener { onEditClick() }
            } else {
                btnEdit.visibility = View.GONE
            }
        }
    }

    class RecommendedHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvRecommendedTitle)

        fun bind() {
            tvTitle.setText(R.string.recommended)
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
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)

        fun bind(menu: MenuItemModel, onItemClick: (MenuItemModel) -> Unit) {
            tvName.text = menu.name ?: itemView.context.getString(R.string.no_name)

            if (!menu.details.isNullOrEmpty()) {
                tvDescription.text = menu.details
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            tvPrice.text = String.format(itemView.context.getString(R.string.currency_format), menu.price)

            Glide.with(itemView.context)
                .load(menu.image_url)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imgMenu)

            itemView.setOnClickListener { onItemClick(menu) }
            btnEdit.setOnClickListener { onItemClick(menu) }
        }
    }
}