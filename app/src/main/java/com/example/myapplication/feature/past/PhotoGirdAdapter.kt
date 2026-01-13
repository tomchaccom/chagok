package com.example.myapplication.feature.past

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.card.MaterialCardView

// 데이터 클래스 예시 (이미지 URI 또는 리소스 ID)
data class PhotoItem(val id: String, val imageUrl: String)

class PhotoGridAdapter(
    private val photos: List<PhotoItem>,
    private val onPhotoClick: (PhotoItem) -> Unit
) : RecyclerView.Adapter<PhotoGridAdapter.PhotoViewHolder>() {

    // 현재 선택된 아이템의 위치 (-1은 선택 안 됨을 의미)
    private var selectedPosition = -1

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: MaterialCardView = itemView.findViewById(R.id.cardRoot)
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)

        fun bind(photo: PhotoItem, position: Int) {
            // 1. 이미지 로드 (Glide 등 사용 권장)
            // Glide.with(itemView).load(photo.imageUrl).into(ivPhoto)
            ivPhoto.setImageResource(R.drawable.ic_launcher_background) // 임시 이미지

            // 2. 선택 상태에 따른 테두리 디자인 변경 (핵심 로직!)
            if (position == selectedPosition) {
                // [선택됨] 연한 초록색(#80E1A6) 테두리 3dp
                cardRoot.strokeWidth = 8 // 굵기 (px 단위, 필요시 dp로 변환)
                cardRoot.strokeColor = Color.parseColor("#80E1A6")
                cardRoot.alpha = 1.0f
            } else {
                // [선택 안 됨] 테두리 없음
                cardRoot.strokeWidth = 0
                cardRoot.strokeColor = Color.TRANSPARENT
                cardRoot.alpha = 0.8f // 선택 안 된건 살짝 흐리게 (선택사항)
            }

            // 3. 클릭 이벤트
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition // 현재 위치를 선택된 위치로 저장

                // 성능 최적화: 이전 선택된 녀석과 지금 선택된 녀석만 새로고침
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onPhotoClick(photo) // 프래그먼트로 클릭 전달
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_grid, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], position)
    }

    override fun getItemCount(): Int = photos.size
}