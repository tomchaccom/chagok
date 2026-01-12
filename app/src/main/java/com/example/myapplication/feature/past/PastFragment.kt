package com.example.myapplication.feature.past

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R
import com.example.myapplication.data.past.PastRepository

class PastFragment : Fragment() {

    private lateinit var viewModel: PastViewModel

    private lateinit var rvDays: androidx.recyclerview.widget.RecyclerView
    private lateinit var rvPhotos: androidx.recyclerview.widget.RecyclerView
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
        // View 참조
        rvDays = view.findViewById(R.id.rvDays)
        rvPhotos = view.findViewById(R.id.rvPhotos)
        detailContainer = view.findViewById(R.id.detailContainer)
        btnBack = view.findViewById(R.id.btnBack)
        tvDetailDate = view.findViewById(R.id.tvDetailDate)
        tvMemoContent = view.findViewById(R.id.tvMemoContent)
        tvMemoTitle = view.findViewById(R.id.tvMemoTitle)

        // 상태표시줄 등 시스템 인셋으로 뷰가 가려지는 문제 해결: 루트 뷰에 상단 인셋을 적용
        val rootView = view.findViewById<View>(R.id.root)
        // 초기 패딩을 저장해서 인셋 이벤트가 여러 번 발생해도 누적되지 않도록 함
        val initialPaddingTop = rootView.paddingTop
        val initialPaddingLeft = rootView.paddingLeft
        val initialPaddingRight = rootView.paddingRight
        val initialPaddingBottom = rootView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(initialPaddingLeft, initialPaddingTop + statusBarTop, initialPaddingRight, initialPaddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootView)

        // ViewModel: repository 주입용 factory 사용 (없으면 Hilt 또는 기본 생성자 사용)
        val repo = PastRepository(requireContext())
        val factory = com.example.myapplication.feature.past.PastViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(PastViewModel::class.java)

        // Day list
        dayAdapter = DayAdapter { day ->
            viewModel.selectDay(day)
            showDetailFor(day)
        }
        // stable id는 adapter에 설정
        dayAdapter.setHasStableIds(true)
        rvDays.layoutManager = LinearLayoutManager(requireContext())
        rvDays.setHasFixedSize(true)
        rvDays.setItemViewCacheSize(20)
        rvDays.adapter = dayAdapter

        // 스크롤 중에는 이미지 로더 일시중지하여 프레임 드랍 방지
        rvDays.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING,
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING -> {
                        com.example.myapplication.util.ImageLoader.setPaused(true)
                    }
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE -> {
                        com.example.myapplication.util.ImageLoader.setPaused(false)
                        // 보이는 항목만 RecyclerView의 바인딩 흐름을 통해 다시 그리도록 notifyItemChanged 사용
                        val lm = recyclerView.layoutManager as? LinearLayoutManager
                        if (lm != null) {
                            val start = lm.findFirstVisibleItemPosition()
                            val end = lm.findLastVisibleItemPosition()
                            if (start in 0..end) {
                                // UI 스레드에서 안전하게 실행
                                recyclerView.post {
                                    for (i in start..end) {
                                        if (i < dayAdapter.itemCount) {
                                            dayAdapter.notifyItemChanged(i)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })

        // Photo grid
        photoAdapter = PhotoAdapter { position ->
            viewModel.togglePhoto(position)
        }
        // stable id는 adapter에 설정
        photoAdapter.setHasStableIds(true)
        rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)
        rvPhotos.setHasFixedSize(true)
        rvPhotos.setItemViewCacheSize(30)
        rvPhotos.adapter = photoAdapter

        btnBack.setOnClickListener {
            viewModel.clearDay()
            showList()
        }

        // Observe
        viewModel.days.observe(viewLifecycleOwner) { days ->
            // submitList는 DiffUtil을 사용하므로 부분 갱신만 발생합니다.
            dayAdapter.submitList(days)
        }

        viewModel.selectedDay.observe(viewLifecycleOwner) { day ->
            if (day == null) showList() else showDetailFor(day)
        }

        viewModel.selectedPhotoIndex.observe(viewLifecycleOwner) { idx ->
            val day = viewModel.selectedDay.value
            if (day == null) {
                tvMemoTitle.text = ""
                tvMemoContent.text = ""
            } else {
                if (idx == null) {
                    tvMemoTitle.text = "오늘 메모"
                    tvMemoContent.text = day.dayMemo
                } else {
                    tvMemoTitle.text = "사진 메모"
                    tvMemoContent.text = day.photos.getOrNull(idx)?.memo ?: day.dayMemo
                }
            }
            photoAdapter.setSelectedIndex(idx)
        }
    }

    private fun showList() {
        detailContainer.visibility = View.GONE
        rvDays.visibility = View.VISIBLE
    }

    private fun showDetailFor(day: com.example.myapplication.data.past.DayEntry) {
        rvDays.visibility = View.GONE
        detailContainer.visibility = View.VISIBLE
        tvDetailDate.text = day.dateLabel
        tvMemoTitle.text = "오늘 메모"
        tvMemoContent.text = day.dayMemo
        photoAdapter.submitList(day.photos)
    }
}
