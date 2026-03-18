package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PromotionSliderAdapter(
    private val promotions: List<PromotionModel>
) : RecyclerView.Adapter<PromotionSliderAdapter.PromotionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promotion, parent, false)
        return PromotionViewHolder(view)
    }

    override fun getItemCount(): Int = promotions.size

    override fun onBindViewHolder(holder: PromotionViewHolder, position: Int) {
        holder.bind(promotions[position])
    }

    class PromotionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPromotion: ImageView = itemView.findViewById(R.id.imgPromotion)
        private val tvName: TextView = itemView.findViewById(R.id.tvPromotionName)


        fun bind(promotion: PromotionModel) {
            tvName.text = promotion.pro_name ?: "โปรโมชั่น"

            Glide.with(itemView.context)
                .load(promotion.img_url)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imgPromotion)
        }
    }
}