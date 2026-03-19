package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrderShopAdapter(
    private val orders: List<Order>,
    private val onItemClick: (Order) -> Unit,
    private val onConfirmClick: ((Order) -> Unit)? = null,
    private val onRejectClick: ((Order) -> Unit)? = null
) : RecyclerView.Adapter<OrderShopAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_shop, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int = orders.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderNumber: TextView = itemView.findViewById(R.id.tvOrderNumber)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val tvOrderItems: TextView = itemView.findViewById(R.id.tvOrderItems)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvPickupTime: TextView = itemView.findViewById(R.id.tvPickupTime)
        private val layoutActions: LinearLayout = itemView.findViewById(R.id.layoutActions)
        private val tvCompletedStatus: TextView = itemView.findViewById(R.id.tvCompletedStatus)
        private val tvViewDetails: TextView = itemView.findViewById(R.id.tvViewDetails)
        private val btnConfirm: Button = itemView.findViewById(R.id.btnConfirm)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)

        fun bind(order: Order) {
            tvOrderNumber.text = order.orderId
            tvOrderItems.text = formatOrderItems(order.items)


            tvCustomerName.text = "Customer: ${order.username}"

            tvPickupTime.text = "Pickup: ${order.pickupTime}"

            // Set status
            when (order.status) {
                "pending" -> {
                    tvOrderStatus.text = "Waiting for Confirmation"
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    layoutActions.visibility = View.VISIBLE
                    tvCompletedStatus.visibility = View.GONE
                }
                "confirmed" -> {
                    tvOrderStatus.text = "Preparing"
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                    layoutActions.visibility = View.GONE
                    tvCompletedStatus.visibility = View.GONE
                }
                "completed" -> {
                    tvOrderStatus.text = "Completed"
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    layoutActions.visibility = View.GONE
                    tvCompletedStatus.visibility = View.VISIBLE
                }
                else -> {
                    tvOrderStatus.text = order.status
                    layoutActions.visibility = View.GONE
                    tvCompletedStatus.visibility = View.GONE
                }
            }

            // Click listeners
            tvViewDetails.setOnClickListener {
                onItemClick(order)
            }

            btnConfirm.setOnClickListener {
                onConfirmClick?.invoke(order)
            }

            btnReject.setOnClickListener {
                onRejectClick?.invoke(order)
            }
        }

        private fun formatOrderItems(items: List<CartItem>): String {
            return items.joinToString(", ") { "${it.name} x${it.quantity}" }
        }
    }
}

        private fun formatOrderItems(items: List<CartItem>): String {
            return items.joinToString(", ") { "${it.name} x${it.quantity}" }
        }
