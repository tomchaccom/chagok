package com.example.myapplication.feature.present

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// 1. 바인딩 클래스 임포트 확인 (자동 임포트가 안 되면 수동으로 경로 확인)
import com.example.myapplication.databinding.ItemTodayGoalBinding

// 목표 데이터 클래스 (ViewModel에서 사용하는 클래스 타입과 일치해야 함)
data class TodayGoal(
    val id: String,
    val title: String,
    val isAchieved: Boolean = false
)

class TodayGoalAdapter(
    private val onAction: (TodayGoal, Boolean) -> Unit
) : ListAdapter<TodayGoal, TodayGoalAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 2. 바인딩 인스턴스 생성 방식 확인
        val binding = ItemTodayGoalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemTodayGoalBinding) :
        RecyclerView.ViewHolder(binding.root) { // 3. binding.root가 정확히 View 타입인지 확인

        fun bind(item: TodayGoal) {
            // 4. XML의 ID가 tvTodayGoalTitle, btnComplete로 되어 있는지 확인
            binding.tvTodayGoalTitle.text = item.title

            binding.btnComplete.setOnClickListener {
                onAction(item, true)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TodayGoal>() {
            override fun areItemsTheSame(oldItem: TodayGoal, newItem: TodayGoal): Boolean =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: TodayGoal, newItem: TodayGoal): Boolean =
                oldItem == newItem
        }
    }
}