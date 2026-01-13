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

        fun bind(photo: DailyRecord, position: Int) {
            // 썸네일 사이즈로 로드 (이미지뷰 높이 110dp -> 픽셀로 변환)
            val density = itemView.context.resources.displayMetrics.density
            val hPx = (110 * density).toInt()
            ImageLoader.loadInto(ivPhoto, photo.photoUri, R.drawable.ic_launcher_background, reqWidth = hPx, reqHeight = hPx)
            ivPhoto.visibility = View.VISIBLE

            // 선택 여부 확인
            val isSelected = (selectedIndex == position)

            // [수정 1] 사진 위를 덮는 초록색 막은 이제 필요 없으니 숨깁니다.
            overlay.isVisible = false

            // [수정 2] 테두리 설정 (CardView 속성 이용)
            if (isSelected) {
                // 두께를 4 -> 2 로 줄임 (원하시는 두께로 숫자 조절 가능)
                cardRoot.strokeWidth = (2 * density).toInt()

                // 색상은 그대로 유지
                cardRoot.strokeColor = Color.parseColor("#80E1A6")
            } else {
                // 선택 안 됐을 때는 테두리 없음
                cardRoot.strokeWidth = 0
            }
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
