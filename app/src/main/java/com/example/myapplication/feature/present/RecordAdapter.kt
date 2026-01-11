package com.example.myapplication.feature.present

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.core.util.ImageUtils
import com.example.myapplication.databinding.ItemRecordBinding

class RecordAdapter(
    private val onMainChecked: (DailyRecord, Boolean) -> Unit
) : ListAdapter<DailyRecord, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: DailyRecord) {
            // 메모 표시
            binding.recordMemo.text = if (record.memo.isNotEmpty()) record.memo else "(메모 없음)"

            // 점수 표시
            binding.recordScoreBadge.text = String.format("⭐ %d", record.score)

            // 날짜 표시
            binding.recordDate.text = record.date

            // 기억/잊기 배지 표시
            val meaningBadge = binding.meaningBadge
            if (record.meaning == Meaning.REMEMBER) {
                meaningBadge.text = "✅ 기억"
                meaningBadge.setBackgroundColor(binding.root.context.getColor(R.color.primary))
                meaningBadge.setTextColor(binding.root.context.getColor(android.R.color.white))
            } else {
                meaningBadge.text = "❌ 잊기"
                meaningBadge.setBackgroundColor(binding.root.context.getColor(R.color.error))
                meaningBadge.setTextColor(binding.root.context.getColor(android.R.color.white))
            }

            // 사진 로드
            if (record.photoUri.isNotEmpty()) {
                try {
                    val uri = record.photoUri.toUri()
                    val correctedBitmap = ImageUtils.fixImageOrientation(binding.root.context, uri)
                    if (correctedBitmap != null) {
                        binding.recordPhoto.setImageBitmap(correctedBitmap)
                    } else {
                        binding.recordPhoto.setImageURI(uri)
                    }
                } catch (e: Exception) {
                    // URI 파싱 실패 시 기본 이미지 설정
                    binding.recordPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }

            // 대표 기억 체크박스는 '의도'만 전달하고, 상태는 데이터 바인딩으로 유지합니다.
            binding.recordMainCheckbox.setOnCheckedChangeListener(null)
            binding.recordMainCheckbox.isChecked = record.isFeatured
            binding.recordMainCheckbox.setOnClickListener {
                val shouldSelect = !record.isFeatured
                onMainChecked(record, shouldSelect)
                // 즉시 토글되지 않도록 기존 상태로 되돌려 UI-상태 불일치를 방지합니다.
                binding.recordMainCheckbox.isChecked = record.isFeatured
            }
        }
    }
}

class RecordDiffCallback : DiffUtil.ItemCallback<DailyRecord>() {
    override fun areItemsTheSame(oldItem: DailyRecord, newItem: DailyRecord): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DailyRecord, newItem: DailyRecord): Boolean {
        return oldItem == newItem
    }
}
