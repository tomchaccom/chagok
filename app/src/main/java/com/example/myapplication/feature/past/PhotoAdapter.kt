package com.example.myapplication.feature.past

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.present.DailyRecord
import com.example.myapplication.util.ImageLoader

class PhotoAdapter(
    private val onPhotoClick: (position: Int) -> Unit
) : ListAdapter<DailyRecord, PhotoAdapter.PhotoVH>(PhotoDiffCallback()) {

    private var selectedIndex: Int? = null

    fun setSelectedIndex(index: Int?) {
        val prev = selectedIndex
        selectedIndex = index
        // 부분 갱신: 이전/현재 선택 항목만 갱신
        if (prev != null) notifyItemChanged(prev)
        if (index != null) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_grid, parent, false)
        return PhotoVH(v)
    }

    override fun onBindViewHolder(holder: PhotoVH, position: Int) {
        holder.bind(getItem(position), position)
    }

    override fun getItemId(position: Int): Long {
        return try { getItem(position).id.hashCode().toLong() } catch (_: Exception) { position.toLong() }
    }

    inner class PhotoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val overlay: View = itemView.findViewById(R.id.overlay)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onPhotoClick(pos)
                }
            }
        }

        fun bind(photo: DailyRecord, position: Int) {
            // 썸네일 사이즈로 로드 (이미지뷰 높이 110dp -> 픽셀로 변환)
            val density = itemView.context.resources.displayMetrics.density
            val hPx = (110 * density).toInt()
            ImageLoader.loadInto(ivPhoto, photo.photoUri, R.drawable.ic_launcher_background, reqWidth = hPx, reqHeight = hPx)
            ivPhoto.visibility = View.VISIBLE

            val isSelected = (selectedIndex == position)
            overlay.isVisible = isSelected
            overlay.setBackgroundColor(if (isSelected) Color.parseColor("#330000FF") else Color.TRANSPARENT)
        }
    }
}

private class PhotoDiffCallback : DiffUtil.ItemCallback<DailyRecord>() {
    override fun areItemsTheSame(oldItem: DailyRecord, newItem: DailyRecord): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DailyRecord, newItem: DailyRecord): Boolean {
        return oldItem == newItem
    }
}
