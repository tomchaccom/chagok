package com.example.myapplication.feature.present

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// 🌟 R 클래스 임포트 필수 (패키지명 확인)
import com.example.myapplication.R
import com.example.myapplication.data.future.Goal as DataGoal

class TodayGoalAdapter(
    private val onActionClick: (DataGoal) -> Unit
) : ListAdapter<DataGoal, TodayGoalAdapter.VH>(GoalDiff) {

    // 🌟 onCreateViewHolder 필수 구현
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_today_goal, parent, false)
        return VH(view)
    }

    // 🌟 onBindViewHolder 필수 구현
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(goal: DataGoal) {
            val titleTv = view.findViewById<TextView>(R.id.tvTodayGoalTitle)
            val actionBtn = view.findViewById<ImageButton>(R.id.btnComplete)

            titleTv.text = goal.title

            if (goal.isAchieved) {
                titleTv.paintFlags = titleTv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                view.alpha = 0.5f
                actionBtn.setImageResource(android.R.drawable.checkbox_on_background)
                actionBtn.isEnabled = false
            } else {
                titleTv.paintFlags = titleTv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                view.alpha = 1.0f
                actionBtn.setImageResource(R.drawable.ic_chevron_right)
                actionBtn.isEnabled = true
                actionBtn.setOnClickListener { onActionClick(goal) }
            }
        }
    }

    companion object GoalDiff : DiffUtil.ItemCallback<DataGoal>() {
        override fun areItemsTheSame(oldItem: DataGoal, newItem: DataGoal) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DataGoal, newItem: DataGoal) = oldItem == newItem
    }
}