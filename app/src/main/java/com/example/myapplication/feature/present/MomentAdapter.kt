package com.example.myapplication.feature.present

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemMomentCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ❌ 삭제됨: data class DailyRecord(...)
// (이제 DailyModels.kt에 있는 클래스를 자동으로 가져다 씁니다)

class MomentAdapter(
    private val onEditClick: ((DailyRecord) -> Unit)? = null
) : RecyclerView.Adapter<MomentAdapter.MomentViewHolder>() {

    private var items: List<DailyRecord> = emptyList()


    fun setItems(newItems: List<DailyRecord>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MomentViewHolder, position: Int) {
        holder.bind(items[position])
    }
    inner class MomentViewHolder(private val binding: ItemMomentCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: DailyRecord) {
            // 1. 이미지 로드
            if (record.photoUri.isNotEmpty()) {
                binding.ivMomentPhoto.setImageURI(Uri.parse(record.photoUri))
            } else {
                // 이미지가 없을 경우 처리 (필요시 구현)
                binding.ivMomentPhoto.setImageDrawable(null)
            }

            // 2. 점수 설정 (수정됨: cesMetrics 안에 있는 weightedScore 사용)
            binding.tvCesScore.text = record.cesMetrics.weightedScore.toString()

            // 3. 메모 설정
            binding.tvMemo.text = if (record.memo.isNotBlank()) record.memo else "메모가 없습니다."

            val isEditableToday = isToday(record.date)
            binding.editMomentButton.isVisible = isEditableToday
            binding.editMomentButton.setOnClickListener(null)
            if (isEditableToday) {
                binding.editMomentButton.setOnClickListener {
                    onEditClick?.invoke(record)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MomentViewHolder {
        val binding = ItemMomentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MomentViewHolder(binding)
    }

    fun submitList(newItems: List<DailyRecord>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun isToday(date: String): Boolean {
        if (date.isBlank()) {
            return false
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return date == today
    }
}
