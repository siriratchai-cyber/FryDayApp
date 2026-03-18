package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CustomerOrderAdapter(
    private var orders: List<Order>,
    private val onItemClick: (Order) -> Unit
) : RecyclerView.Adapter<CustomerOrderAdapter.OrderViewHolder>() {

    fun updateItems(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int = orders.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardOrder: CardView = itemView.findViewById(R.id.cardOrder)
        private val tvOrderNumber: TextView = itemView.findViewById(R.id.tvOrderNumber)
        private val tvOrderItems: TextView = itemView.findViewById(R.id.tvOrderItems)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val tvStatusMessage: TextView = itemView.findViewById(R.id.tvStatusMessage)

        fun bind(order: Order) {
            tvOrderNumber.text = "Order ${order.orderId}"

            // แสดงรายการอาหาร
            val itemsText = order.items.joinToString("\n") {
                "${it.name} x${it.quantity}"
            }
            tvOrderItems.text = itemsText

            // แสดงสถานะตาม order.status
            when (order.status) {
                "pending" -> {
                    tvOrderStatus.text = "⏳ pending"
                    tvStatusMessage.text = "Waiting for confirmation"
                    tvOrderStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.orange))
                }
                "confirmed" -> {
                    tvOrderStatus.text = "✅ confirmed"
                    tvStatusMessage.text = "Restaurant is preparing ingredients"
                    tvOrderStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.orange))
                }
                "preparing" -> {
                    tvOrderStatus.text = "👨‍🍳 preparing"
                    tvStatusMessage.text = "The restaurant is cooking"
                    tvOrderStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.orange))
                }
                "completed" -> {
                    tvOrderStatus.text = "✨ completed"
                    tvStatusMessage.text = "Food is ready for pickup"
                    tvOrderStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                }
                "rejected" -> {
                    tvOrderStatus.text = "❌ rejected"
                    tvStatusMessage.text = "Order rejected, please contact restaurant"
                    tvOrderStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                }
                else -> {
                    tvOrderStatus.text = order.status
                    tvStatusMessage.text = ""
                }
            }

            cardOrder.setOnClickListener {
                onItemClick(order)
            }
        }
    }
}