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

// 🌟 Alias 설정: data 패키지의 모델을 사용하도록 강제함
import com.example.myapplication.data.present.DailyRecord as DataDailyRecord
import com.example.myapplication.data.present.Meaning as DataMeaning

class RecordAdapter(
    private val onEditClick: (DataDailyRecord) -> Unit // 🌟 인자 타입 변경
) : ListAdapter<DataDailyRecord, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) { // 🌟 제네릭 타입 변경

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: DataDailyRecord) { // 🌟 타입 변경
            val context = binding.root.context

            binding.recordMemo.text = record.memo.ifEmpty { "(메모 없음)" }
            binding.recordCesValue.text = String.format(Locale.getDefault(), "CES %.1f", record.cesMetrics.weightedScore)
            binding.recordDate.text = record.date

            binding.btnEditRecord.setOnClickListener { onEditClick(record) }

            // 🌟 DataMeaning 별칭 사용
            val isRemember = record.meaning == DataMeaning.REMEMBER
            binding.meaningBadge.apply {
                text = if (isRemember) "✅ 기억" else "❌ 잊기"
                val colorRes = if (isRemember) R.color.primary else R.color.error

                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(context, colorRes)
                )
            }

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
            } else {
                binding.recordPhoto.setImageResource(R.drawable.chagok_pic2)
            }
        }
    }
}

// 🌟 DiffUtil의 제네릭 타입도 변경
class RecordDiffCallback : DiffUtil.ItemCallback<DataDailyRecord>() {
    override fun areItemsTheSame(oldItem: DataDailyRecord, newItem: DataDailyRecord) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DataDailyRecord, newItem: DataDailyRecord) = oldItem == newItem
}