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

    // PhotoAdapter.kt 내부의 PhotoVH 클래스

    inner class PhotoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val overlay: View = itemView.findViewById(R.id.overlay)

        // ★ 추가: 최상위 레이아웃인 CardView를 가져옵니다.
        // (XML에서 root가 MaterialCardView이므로 itemView를 형변환)
        private val cardRoot = itemView as com.google.android.material.card.MaterialCardView

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onPhotoClick(pos)
                }
            }
        }

        fun bind(photo: PhotoItem, position: Int) {
            // ... (이미지 로드 코드는 기존과 동일) ...
            val density = itemView.context.resources.displayMetrics.density
            val hPx = (110 * density).toInt()
            ImageLoader.loadInto(ivPhoto, photo.imageUri, R.drawable.ic_launcher_background, reqWidth = hPx, reqHeight = hPx)
            ivPhoto.visibility = View.VISIBLE

            // 선택 여부 확인
            val isSelected = (selectedIndex == position)

            // 1. 오버레이 (연한 초록색 빛) 보이기/숨기기
            overlay.isVisible = isSelected

            // 2. ★ 핵심 수정 ★: 테두리는 CardView가 직접 그립니다.
            if (isSelected) {
                cardRoot.strokeWidth = (4 * density).toInt() // 두께 4dp
                cardRoot.strokeColor = Color.parseColor("#80E1A6") // 선명한 연두색
            } else {
                cardRoot.strokeWidth = 0 // 선택 안 되면 테두리 없음
            }
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
