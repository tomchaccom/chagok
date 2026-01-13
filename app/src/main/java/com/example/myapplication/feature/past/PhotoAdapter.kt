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
import com.example.myapplication.data.past.PhotoItem
import com.example.myapplication.util.ImageLoader

class PhotoAdapter(
    private val onPhotoClick: (position: Int) -> Unit
) : ListAdapter<PhotoItem, PhotoAdapter.PhotoVH>(PhotoDiffCallback()) {

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
        return try { getItem(position).imageUri.hashCode().toLong() } catch (_: Exception) { position.toLong() }
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

        fun bind(photo: PhotoItem, position: Int) {
            // ... (이미지 로드 코드는 그대로 유지) ...

            val density = itemView.context.resources.displayMetrics.density
            val hPx = (110 * density).toInt()
            ImageLoader.loadInto(ivPhoto, photo.imageUri, R.drawable.ic_launcher_background, reqWidth = hPx, reqHeight = hPx)
            ivPhoto.visibility = View.VISIBLE

            val isSelected = (selectedIndex == position)

            // [수정] 배경색 변경 코드를 삭제하고 visibility만 조절합니다.
            // XML에 설정된 @drawable/bg_photo_selected가 보이게 됩니다.
            overlay.isVisible = isSelected
        }
    }
}

private class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoItem>() {
    override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem.imageUri == newItem.imageUri
    }

    override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
        return oldItem == newItem
    }
}
