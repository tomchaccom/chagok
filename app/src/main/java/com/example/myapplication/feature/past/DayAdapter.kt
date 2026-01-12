package com.example.myapplication.feature.past

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.R
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.data.past.PhotoItem

class DayAdapter(
    private val onClick: (DayEntry) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayVH>() {

    private var items: List<DayEntry> = emptyList()

    fun submitList(list: List<DayEntry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_entry, parent, false)
        return DayVH(v)
    }

    override fun onBindViewHolder(holder: DayVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DayVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvMemo: TextView = itemView.findViewById(R.id.tvMemo)

        init {
            itemView.setOnClickListener {
                onClick(items[adapterPosition])
            }
        }

        fun bind(day: DayEntry) {
            tvDate.text = day.dateLabel
            tvMemo.text = day.dayMemo
            val rep: PhotoItem? = day.representativePhoto
            if (rep != null) {
                // Coil로 imageUri 로드 (android.resource 등 모두 처리)
                ivThumb.load(rep.imageUri) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background)
                }
            } else {
                ivThumb.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }
    }
}
