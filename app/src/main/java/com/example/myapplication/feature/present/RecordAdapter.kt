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
import java.util.Locale

class RecordAdapter(
    private val onEditClick: (DailyRecord) -> Unit // 수정 콜백
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
            val context = binding.root.context

            // 1. 기본 텍스트 정보 반영
            binding.recordMemo.text = record.memo.ifEmpty { "(메모 없음)" }
            binding.recordCesValue.text = String.format(Locale.getDefault(), "CES %.1f", record.cesMetrics.weightedScore)
            binding.recordDate.text = record.date

            // 2. 수정 버튼 리스너
            binding.btnEditRecord.setOnClickListener { onEditClick(record) }

            // 3. 배지 색상 및 텍스트 설정
            // bind 함수 내부 수정
            val isRemember = record.meaning == Meaning.REMEMBER
            binding.meaningBadge.apply {
                text = if (isRemember) "✅ 기억" else "❌ 잊기"

                // R.id.primary -> R.color.primary (또는 프로젝트에 정의된 색상 이름)
                val colorRes = if (isRemember) R.color.primary else R.color.error

                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(context, colorRes)
                )
            }

            // 4. 사진 로드 (메모리 효율을 위해 Glide 권장, 없다면 기존 로직 유지)
            if (record.photoUri.isNotEmpty()) {
                try {
                    val uri = record.photoUri.toUri()
                    // 보정된 비트맵을 가져오되, null일 경우 URI로 직접 로드
                    val correctedBitmap = ImageUtils.fixImageOrientation(context, uri)
                    if (correctedBitmap != null) {
                        binding.recordPhoto.setImageBitmap(correctedBitmap)
                    } else {
                        binding.recordPhoto.setImageURI(uri)
                    }
                } catch (e: Exception) {
                    binding.recordPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                // 사진이 없을 때 기본 이미지 처리
                binding.recordPhoto.setImageResource(R.drawable.chagok_pic2)
            }
        }
    }
}

class RecordDiffCallback : DiffUtil.ItemCallback<DailyRecord>() {
    override fun areItemsTheSame(oldItem: DailyRecord, newItem: DailyRecord) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DailyRecord, newItem: DailyRecord) = oldItem == newItem
}