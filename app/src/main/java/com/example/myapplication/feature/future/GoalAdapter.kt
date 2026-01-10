package com.example.myapplication.feature.future

import com.example.myapplication.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class GoalAdapter : RecyclerView.Adapter<GoalAdapter.VH>() {

    private val items = mutableListOf<Goal>()
    private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN)

    fun submitList(list: List<Goal>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = items[position]
        holder.title.text = g.title
        holder.date.text = dateFormat.format(Date(g.dateMillis))
        // 아이콘은 placeholder로 시스템 drawable 사용 (원하면 교체)
        holder.icon.setImageResource(android.R.drawable.ic_menu_compass)
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.goal_icon)
        val title: TextView = view.findViewById(R.id.goal_title)
        val date: TextView = view.findViewById(R.id.goal_date)
    }
}
