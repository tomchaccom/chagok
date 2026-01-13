package com.example.myapplication.feature.past

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.feature.present.DailyRecord
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.util.ImageLoader

class DayAdapter(
    private val onClick: (DayEntry) -> Unit
) : ListAdapter<DayEntry, DayAdapter.DayVH>(DayDiffCallback()) {

    private var thumbnailPx: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_entry, parent, false)
        return DayVH(v)
    }

    override fun getItemId(position: Int): Long {
        // DayEntry에 포함된 고유 id 사용
        return try { getItem(position).id } catch (_: Throwable) { position.toLong() }
    }

    override fun onBindViewHolder(holder: DayVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DayVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvMemo: TextView = itemView.findViewById(R.id.tvMemo)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(getItem(pos))
                }
            }
        }

        fun bind(day: DayEntry) {
            tvDate.text = day.dateLabel
            // dayMemo는 더 이상 사용하지 않음 — 대표 사진의 memo를 우선 표시
            val rep: DailyRecord? = day.representativePhoto
            tvMemo.text = rep?.memo ?: ""
            val repPhoto: DailyRecord? = rep
            if (repPhoto != null) {
                if (thumbnailPx == 0) {
                    val density = itemView.context.resources.displayMetrics.density
                    thumbnailPx = (56 * density).toInt()
                }
                ImageLoader.loadInto(ivThumb, repPhoto.photoUri, R.drawable.ic_launcher_background, reqWidth = thumbnailPx, reqHeight = thumbnailPx)
            } else {
                ivThumb.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }
    }
}

private class DayDiffCallback : DiffUtil.ItemCallback<DayEntry>() {
    override fun areItemsTheSame(oldItem: DayEntry, newItem: DayEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DayEntry, newItem: DayEntry): Boolean {
        return oldItem == newItem
    }
}
