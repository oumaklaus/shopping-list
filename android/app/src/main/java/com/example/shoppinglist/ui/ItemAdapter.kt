package com.example.shoppinglist.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.Item
import com.example.shoppinglist.databinding.ItemShoppingBinding

class ItemAdapter(
    private val onPurchasedChanged: (Item, Boolean) -> Unit,
    private val onEdit: (Item) -> Unit,
    private val onDelete: (Item) -> Unit
) : ListAdapter<Item, ItemAdapter.ItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemShoppingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemViewHolder(
        private val binding: ItemShoppingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            val context = binding.root.context

            binding.checkboxPurchased.setOnCheckedChangeListener(null)
            binding.checkboxPurchased.isChecked = item.purchased
            binding.checkboxPurchased.contentDescription = context.getString(
                if (item.purchased) R.string.mark_as_unpurchased else R.string.mark_as_purchased
            )
            binding.checkboxPurchased.setOnCheckedChangeListener { _, isChecked ->
                onPurchasedChanged(item, isChecked)
            }

            binding.textName.text = item.name
            binding.textQuantity.text = context.getString(R.string.quantity_format, item.quantity)

            if (item.purchased) {
                binding.textName.paintFlags =
                    binding.textName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.6f
            } else {
                binding.textName.paintFlags =
                    binding.textName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1f
            }

            binding.buttonEdit.contentDescription = context.getString(
                R.string.edit_item_description,
                item.name
            )
            binding.buttonDelete.contentDescription = context.getString(
                R.string.delete_item_description,
                item.name
            )

            binding.buttonEdit.setOnClickListener { onEdit(item) }
            binding.buttonDelete.setOnClickListener { onDelete(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
            oldItem == newItem
    }
}
