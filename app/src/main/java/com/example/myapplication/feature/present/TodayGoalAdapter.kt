package com.example.myapplication.feature.present

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemTodayGoalBinding

// Adapter가 사용하는 데이터 타입을 'Practice'로 통일합니다.
class TodayGoalAdapter(
    private val onCheckedChange: (Practice, Boolean) -> Unit
) : ListAdapter<Practice, TodayGoalAdapter.ViewHolder>(PracticeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodayGoalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTodayGoalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Practice) {
            // 1. XML ID 확인: tvTodayGoalTitle
            binding.tvTodayGoalTitle.text = item.title

            // 2. 완료 버튼 클릭 시 onCheckedChange 호출 (onAction -> onCheckedChange로 수정)
            binding.btnComplete.setOnClickListener {
                onCheckedChange(item, true)
            }
        }
    }

    companion object {
        // DiffUtil의 타입도 Practice로 통일합니다.
        private val PracticeDiffCallback = object : DiffUtil.ItemCallback<Practice>() {
            override fun areItemsTheSame(oldItem: Practice, newItem: Practice): Boolean =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Practice, newItem: Practice): Boolean =
                oldItem == newItem
        }
    }
}