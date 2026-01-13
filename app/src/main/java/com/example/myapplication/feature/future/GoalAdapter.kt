package com.example.myapplication.feature.future

import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
// 🌟 Alias 설정: data 패키지의 Goal을 DataGoal로 명명
import com.example.myapplication.data.future.Goal as DataGoal
import java.time.format.DateTimeFormatter

class GoalAdapter(
    private val onCompleteClick: (DataGoal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.VH>() {

    private val items = mutableListOf<DataGoal>()

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun submitList(list: List<DataGoal>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_goal_future, parent, false)
        return VH(v)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: VH, position: Int) {
        val goal = items[position]
        holder.bind(goal, dateFormatter, onCompleteClick)
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.goal_icon)
        val title: TextView = view.findViewById(R.id.goal_title)
        val date: TextView = view.findViewById(R.id.goal_date)
        val btnComplete: View = view.findViewById(R.id.goal_icon)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(goal: DataGoal, formatter: DateTimeFormatter, onComplete: (DataGoal) -> Unit) {
            title.text = goal.title
            date.text = goal.date.format(formatter)
            icon.setImageResource(R.drawable.chagok_app)

            if (goal.isAchieved) {
                title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                title.setTextColor(Color.LTGRAY)
                itemView.setBackgroundColor(Color.parseColor("#F5F5F5"))
                btnComplete.setOnClickListener(null)
                btnComplete.alpha = 0.3f
            } else {
                title.paintFlags = title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                title.setTextColor(Color.BLACK)
                itemView.setBackgroundColor(Color.WHITE)
                btnComplete.alpha = 1.0f
                btnComplete.setOnClickListener { onComplete(goal) }
            }
        }
    }
}