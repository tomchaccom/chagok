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

            // 1. 메모 및 점수 표시
            binding.recordMemo.text = if (record.memo.isNotEmpty()) record.memo else "(메모 없음)"
            binding.recordCesValue.text = String.format(Locale.getDefault(), "CES %.1f", record.cesMetrics.weightedScore)
            binding.recordDate.text = record.date

            // 2. 수정 버튼 클릭 리스너 연결
            binding.btnEditRecord.setOnClickListener { onEditClick(record) }

            // 3. Meaning Badge 설정 (기억/잊기)
            val meaningBadge = binding.meaningBadge
            if (record.meaning == Meaning.REMEMBER) {
                meaningBadge.text = "✅ 기억"
                meaningBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.primary))
            } else {
                meaningBadge.text = "❌ 잊기"
                meaningBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.error))
            }

            // 4. 사진 로드 (보정 로직 포함)
            if (record.photoUri.isNotEmpty()) {
                try {
                    val uri = record.photoUri.toUri()
                    val correctedBitmap = ImageUtils.fixImageOrientation(context, uri)
                    if (correctedBitmap != null) {
                        binding.recordPhoto.setImageBitmap(correctedBitmap)
                    } else {
                        binding.recordPhoto.setImageURI(uri)
                    }
                } catch (e: Exception) {
                    binding.recordPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }
    }
}

class RecordDiffCallback : DiffUtil.ItemCallback<DailyRecord>() {
    override fun areItemsTheSame(oldItem: DailyRecord, newItem: DailyRecord) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DailyRecord, newItem: DailyRecord) = oldItem == newItem
}