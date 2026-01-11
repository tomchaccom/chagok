package com.example.myapplication.feature.past

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

// --- 데이터 모델 ---
data class PhotoItem(
    val id: Long,
    val title: String,
    val memo: String
    // 실제 앱에선 imageUri: String 또는 Uri를 추가
)

data class DayEntry(
    val dateLabel: String,            // "2024년 3월 20일"
    val dayMemo: String,              // 일자 전체 메모
    val photos: List<PhotoItem>       // 해당 일자의 사진들
) {
    fun representative(): PhotoItem? = photos.firstOrNull()
}

// --- Fragment ---
class PastFragment : Fragment() {

    private lateinit var viewModel: PastViewModel

    // 뷰들
    private lateinit var rvDays: RecyclerView
    private lateinit var rvPhotos: RecyclerView
    private lateinit var detailContainer: View
    private lateinit var btnBack: View
    private lateinit var tvDetailDate: TextView
    private lateinit var tvMemoContent: TextView
    private lateinit var tvMemoTitle: TextView

    private lateinit var dayAdapter: DayAdapter
    private lateinit var photoAdapter: PhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_past, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(PastViewModel::class.java)

        rvDays = view.findViewById(R.id.rvDays)
        rvPhotos = view.findViewById(R.id.rvPhotos)
        detailContainer = view.findViewById(R.id.detailContainer)
        btnBack = view.findViewById(R.id.btnBack)
        tvDetailDate = view.findViewById(R.id.tvDetailDate)
        tvMemoContent = view.findViewById(R.id.tvMemoContent)
        tvMemoTitle = view.findViewById(R.id.tvMemoTitle)

        // Day list setup
        dayAdapter = DayAdapter { day ->
            // 일자 선택 -> 상세 화면으로 전환
            viewModel.selectDay(day)
            showDetailFor(day)
        }
        rvDays.layoutManager = LinearLayoutManager(requireContext())
        rvDays.adapter = dayAdapter

        // Photo grid setup
        photoAdapter = PhotoAdapter(
            onPhotoClick = { photo ->
                viewModel.togglePhotoSelection(photo.id)
            },
            getSelectedId = { viewModel.selectedPhotoId.value }
        )
        // 3열 그리드 (사례) — 필요에 맞게 조절
        rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)
        rvPhotos.adapter = photoAdapter

        // Back
        btnBack.setOnClickListener {
            viewModel.clearSelectedDay()
            showList()
        }

        // Observe days
        viewModel.days.observe(viewLifecycleOwner) { days ->
            dayAdapter.submitList(days)
        }

        // Observe selected day
        viewModel.selectedDay.observe(viewLifecycleOwner) { day ->
            if (day == null) {
                showList()
            } else {
                showDetailFor(day)
            }
        }

        // Observe selected photo to update memo & highlight
        viewModel.selectedPhotoId.observe(viewLifecycleOwner) { selId ->
            val day = viewModel.selectedDay.value
            if (day == null) {
                // safety: 리스트 모드
                tvMemoTitle.text = ""
                tvMemoContent.text = ""
            } else {
                if (selId == null) {
                    // 일자 메모 보여주기
                    tvMemoTitle.text = "사진 메모"
                    tvMemoContent.text = day.dayMemo
                } else {
                    // 사진 메모 보여주기
                    val p = day.photos.find { it.id == selId }
                    if (p != null) {
                        tvMemoTitle.text = "사진 메모"
                        tvMemoContent.text = p.memo
                    } else {
                        tvMemoContent.text = day.dayMemo
                    }
                }
            }
            photoAdapter.setSelectedId(selId)
        }
    }

    // 리스트 화면 보이기
    private fun showList() {
        detailContainer.visibility = View.GONE
        rvDays.visibility = View.VISIBLE
    }

    // 상세 화면 보이기
    private fun showDetailFor(day: DayEntry) {
        rvDays.visibility = View.GONE
        detailContainer.visibility = View.VISIBLE

        tvDetailDate.text = day.dateLabel
        // 기본 메모은 일자 메모(선택된 사진 없을 때)
        tvMemoTitle.text = "사진 메모"
        tvMemoContent.text = day.dayMemo

        // 사진 리스트 채우기
        photoAdapter.submitList(day.photos)
    }

    // --- DayAdapter (일자 리스트) ---
    inner class DayAdapter(private val onClick: (DayEntry) -> Unit) :
        RecyclerView.Adapter<DayAdapter.DayVH>() {

        private var items: List<DayEntry> = emptyList()

        fun submitList(list: List<DayEntry>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_day_entry, parent, false)
            return DayVH(v)
        }

        override fun onBindViewHolder(holder: DayVH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class DayVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
            private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            private val tvMemo: TextView = itemView.findViewById(R.id.tvMemo)
            init {
                itemView.setOnClickListener {
                    onClick(items[adapterPosition])
                }
            }
            fun bind(day: DayEntry) {
                tvDate.text = day.dateLabel
                tvMemo.text = day.dayMemo
                // 대표 이미지는 첫 사진을 간단히 보여줌. 실제 앱에선 URI를 통해 로딩
                val rep = day.representative()
                if (rep != null) {
                    ivThumb.setImageResource(android.R.drawable.ic_menu_gallery)
                } else {
                    ivThumb.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }
        }
    }

    // --- PhotoAdapter (사진 그리드) ---
    inner class PhotoAdapter(
        private val onPhotoClick: (PhotoItem) -> Unit,
        private val getSelectedId: () -> Long?
    ) : RecyclerView.Adapter<PhotoAdapter.PhotoVH>() {

        private var items: List<PhotoItem> = emptyList()
        private var selectedId: Long? = null

        fun submitList(list: List<PhotoItem>) {
            items = list
            notifyDataSetChanged()
        }

        fun setSelectedId(id: Long?) {
            selectedId = id
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_grid, parent, false)
            return PhotoVH(v)
        }

        override fun onBindViewHolder(holder: PhotoVH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class PhotoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
            private val overlay: View = itemView.findViewById(R.id.overlay)
            init {
                itemView.setOnClickListener {
                    val photo = items[adapterPosition]
                    onPhotoClick(photo)
                }
            }
            fun bind(photo: PhotoItem) {
                // 간단 placeholder
                ivPhoto.setImageResource(android.R.drawable.ic_menu_camera)

                // 선택 강조
                val isSelected = (photo.id == selectedId)
                overlay.isVisible = isSelected
                if (isSelected) {
                    overlay.setBackgroundColor(Color.parseColor("#33AAFF"))
                } else {
                    overlay.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }
}
