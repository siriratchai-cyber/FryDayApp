package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PromotionEditAdapter(
    private val promotions: MutableList<PromotionModel>,
    private val onItemClick: (String, PromotionModel) -> Unit
) : RecyclerView.Adapter<PromotionEditAdapter.PromotionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promotion_edit, parent, false)
        return PromotionViewHolder(view)
    }

    override fun getItemCount(): Int = promotions.size

    override fun onBindViewHolder(holder: PromotionViewHolder, position: Int) {
        holder.bind(promotions[position])
    }

    inner class PromotionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPromo: ImageView = itemView.findViewById(R.id.imgPromo)
        private val tvPromoName: TextView = itemView.findViewById(R.id.tvPromoName)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(promotion: PromotionModel) {
            tvPromoName.text = promotion.pro_name

            Glide.with(itemView.context)
                .load(promotion.img_url)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imgPromo)

            btnEdit.setOnClickListener {
                onItemClick("edit", promotion)
            }

            btnDelete.setOnClickListener {
                onItemClick("delete", promotion)
            }
        }
    }
}