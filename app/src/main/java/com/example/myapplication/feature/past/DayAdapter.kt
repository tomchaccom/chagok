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
import com.example.myapplication.data.present.DailyRecord
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.util.ImageLoader

class DayAdapter(
    private val onDayClick: (DayEntry) -> Unit
) : ListAdapter<DayEntry, DayAdapter.DayViewHolder>(DayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        // [수정 전] item_day_record_placeholder -> [수정 후] item_day (또는 작업하신 xml 파일명)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false) // 👈 여기를 수정하세요!
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

        // XML에서 정의한 ID로 변경
        private val tvDateTitle: TextView = itemView.findViewById(R.id.tvDateTitle) // 날짜
        private val tvSummary: TextView = itemView.findViewById(R.id.tvSummary)     // 메모
        private val imgThumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail) // 사진

        fun bind(day: DayEntry) {
            itemView.setOnClickListener { onDayClick(day) }

            // 1. 날짜 표시 (디자인에 맞춰 쪼개지 않고 전체 표시)
            // 예: "2024년 3월 20일" 그대로 사용
            tvDateTitle.text = day.dateLabel

            // 2. 내용(메모) 표시
            // 내용이 없으면 "내용 없음" 같은 기본 문구를 넣을 수도 있습니다.
            tvSummary.text = if (day.dayMemo.isNotEmpty()) day.dayMemo else "기록된 내용이 없습니다."

            // 3. 썸네일 이미지 로드 (기존 로직 유지)
            val repPhoto = day.representativePhoto
            if (repPhoto != null) {
                // 60dp 크기로 로드 (XML의 CardView 크기에 맞춤)
                val sizePx = (60 * itemView.context.resources.displayMetrics.density).toInt()
                ImageLoader.loadInto(
                    imgThumbnail,
                    repPhoto.imageUri,
                    R.drawable.ic_launcher_background,
                    sizePx,
                    sizePx
                )
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
                imgThumbnail.setImageResource(R.drawable.ic_launcher_background)
            }
        }
    }
}
class DayDiffCallback : DiffUtil.ItemCallback<DayEntry>() {
    override fun areItemsTheSame(oldItem: DayEntry, newItem: DayEntry): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DayEntry, newItem: DayEntry): Boolean = oldItem == newItem
}
