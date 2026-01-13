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
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.util.ImageLoader

class DayAdapter(
    private val onDayClick: (DayEntry) -> Unit
) : ListAdapter<DayEntry, DayAdapter.DayViewHolder>(DayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        // 새로 만든 카드 UI 레이아웃 연결
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_record_placeholder, parent, false)
        return DayViewHolder(view, onDayClick)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    class DayViewHolder(
        itemView: View,
        private val onDayClick: (DayEntry) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        private val tvDayOfWeek: TextView = itemView.findViewById(R.id.tvDayOfWeek)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvSummary)
        private val imgThumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail)

        fun bind(day: DayEntry) {
            itemView.setOnClickListener { onDayClick(day) }

            // 날짜 표시 로직 (예: "2024년 3월 20일")
            val dateParts = day.dateLabel.split(" ")
            if (dateParts.size >= 3) {
                // "20" (일)
                tvDayNumber.text = dateParts.last().replace("일", "")
                // "2024년 3월"
                tvDayOfWeek.text = "${dateParts[0]} ${dateParts[1]}"
            } else {
                tvDayNumber.text = day.dateLabel
                tvDayOfWeek.text = ""
            }

            tvSummary.text = day.dayMemo

            // 썸네일 이미지 로드
            val repPhoto = day.representativePhoto
            if (repPhoto != null) {
                val sizePx = (48 * itemView.context.resources.displayMetrics.density).toInt()
                ImageLoader.loadInto(imgThumbnail, repPhoto.imageUri, R.drawable.ic_launcher_background, sizePx, sizePx)
            } else {
                imgThumbnail.setImageResource(R.drawable.ic_launcher_background)
            }
        }
    }
}

class DayDiffCallback : DiffUtil.ItemCallback<DayEntry>() {
    override fun areItemsTheSame(oldItem: DayEntry, newItem: DayEntry): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DayEntry, newItem: DayEntry): Boolean = oldItem == newItem
}