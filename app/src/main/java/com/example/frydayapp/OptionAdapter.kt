package com.example.frydayapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OptionAdapter(
    private val options: List<Option>,
    private val onOptionSelected: (Option, Boolean) -> Unit
) : RecyclerView.Adapter<OptionAdapter.OptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun getItemCount(): Int = options.size

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbOption: CheckBox = itemView.findViewById(R.id.cbOption)
        private val tvOptionName: TextView = itemView.findViewById(R.id.tvOptionName)
        private val tvOptionPrice: TextView = itemView.findViewById(R.id.tvOptionPrice)

        fun bind(option: Option) {
            tvOptionName.text = option.name
            if (option.price > 0) {
                tvOptionPrice.text = "+$${option.price}"
                tvOptionPrice.visibility = View.VISIBLE
            } else {
                tvOptionPrice.visibility = View.GONE
            }

            cbOption.isChecked = option.isSelected

            cbOption.setOnCheckedChangeListener { _, isChecked ->
                onOptionSelected(option.copy(isSelected = isChecked), isChecked)
            }

            itemView.setOnClickListener {
                cbOption.isChecked = !cbOption.isChecked
            }
        }
    }
}