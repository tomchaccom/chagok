package com.example.myapplication.feature.future

import android.os.Build
import com.example.myapplication.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import java.time.format.DateTimeFormatter

class GoalAdapter : RecyclerView.Adapter<GoalAdapter.VH>() {

    private val items = mutableListOf<Goal>()
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter =
        DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun submitList(list: List<Goal>) {
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
        val g = items[position]
        holder.title.text = g.title
        holder.date.text = g.date.format(dateFormatter)

        // [수정] 시스템 아이콘(나침반) 대신 우리 앱 아이콘(chagok_app)으로 교체합니다.
        holder.icon.setImageResource(R.drawable.chagok_app)
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.goal_icon)
        val title: TextView = view.findViewById(R.id.goal_title)
        val date: TextView = view.findViewById(R.id.goal_date)
    }
}
