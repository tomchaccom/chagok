package com.example.myapplication.feature.present

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemPracticeBinding

class PracticeAdapter(
    private val onPracticeStateChanged: (Practice, Boolean) -> Unit
) : ListAdapter<Practice, PracticeAdapter.PracticeViewHolder>(PracticeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PracticeViewHolder {
        val binding = ItemPracticeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PracticeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PracticeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PracticeViewHolder(private val binding: ItemPracticeBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.buttonToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    val practice = getItem(absoluteAdapterPosition)
                    val isAchieved = checkedId == binding.achieveButton.id
                    onPracticeStateChanged(practice, isAchieved)
                }
            }
        }

        fun bind(practice: Practice) {
            binding.practiceTitle.text = practice.title
            binding.practiceSubtitle.text = practice.subtitle

            when (practice.isAchieved) {
                true -> binding.buttonToggleGroup.check(binding.achieveButton.id)
                false -> binding.buttonToggleGroup.check(binding.unachieveButton.id)
                null -> binding.buttonToggleGroup.clearChecked()
            }
        }
    }
}

class PracticeDiffCallback : DiffUtil.ItemCallback<Practice>() {
    override fun areItemsTheSame(oldItem: Practice, newItem: Practice): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Practice, newItem: Practice): Boolean {
        return oldItem == newItem
    }
}