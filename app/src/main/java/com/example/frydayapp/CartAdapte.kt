package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CartAdapter(
    private var items: List<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onItemRemoved: (CartItem) -> Unit,
    private val onItemClicked: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    fun updateItems(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgItem: ImageView = itemView.findViewById(R.id.imgCartItem)
        private val tvName: TextView = itemView.findViewById(R.id.tvCartItemName)
        private val tvOptions: TextView = itemView.findViewById(R.id.tvCartItemOptions)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvCartItemPrice)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvCartQuantity)
        private val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecreaseCart)
        private val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncreaseCart)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveCart)

        fun bind(item: CartItem) {
            tvName.text = item.name
            tvPrice.text = String.format("$%.2f", item.price)
            tvQuantity.text = item.quantity.toString()

            // แสดง options ที่เลือก
            val optionsText = if (item.options.isNotEmpty()) {
                item.options.joinToString(", ") { it.name }
            } else {
                "No options"
            }
            tvOptions.text = optionsText

            // โหลดรูปภาพด้วย Glide (ใช้ URL จริงจากเมนู)
            Glide.with(itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imgItem)

            btnDecrease.setOnClickListener {
                if (item.quantity > 1) {
                    onQuantityChanged(item, item.quantity - 1)
                }
            }

            btnIncrease.setOnClickListener {
                onQuantityChanged(item, item.quantity + 1)
            }

            btnRemove.setOnClickListener {
                onItemRemoved(item)
            }

            itemView.setOnClickListener {
                onItemClicked(item)
            }
        }
    }
}