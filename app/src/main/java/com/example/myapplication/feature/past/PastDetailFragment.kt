package com.example.myapplication.feature.past

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.past.PastRepository

class PastDetailFragment : Fragment() {

    private lateinit var viewModel: PastViewModel
    private lateinit var rvPhotos: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var tvDetailDate: TextView
    private lateinit var tvMemoTitle: TextView
    private lateinit var tvMemoContent: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 앞서 만든 fragment_past_detail.xml 레이아웃 사용
        return inflater.inflate(R.layout.fragment_past_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰 연결
        rvPhotos = view.findViewById(R.id.rvPhotos)
        tvDetailDate = view.findViewById(R.id.tvDetailDate)
        tvMemoTitle = view.findViewById(R.id.tvMemoTitle)
        tvMemoContent = view.findViewById(R.id.tvMemoContent)
        btnBack = view.findViewById(R.id.btnBack)

        // 시스템 바(상단바) 여백 처리
        setupWindowInsets(view)

        // [중요] ViewModel 공유: requireActivity()를 사용해야 리스트에서 선택한 데이터를 가져올 수 있음
        val repo = PastRepository(requireContext())
        val factory = PastViewModelFactory(repo)
        viewModel = ViewModelProvider(requireActivity(), factory).get(PastViewModel::class.java)

        // 사진 어댑터 설정
        photoAdapter = PhotoAdapter { position ->
            viewModel.togglePhoto(position)
        }
        rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)
        rvPhotos.adapter = photoAdapter

        // 뒤로가기 버튼: 프래그먼트 스택에서 제거 (리스트로 복귀)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 데이터 관찰 (선택된 날짜 정보 표시)
        viewModel.selectedDay.observe(viewLifecycleOwner) { day ->
            if (day != null) {
                tvDetailDate.text = day.dateLabel
                photoAdapter.submitList(day.photos)

                // 사진 선택이 안 되어있으면 기본 메모 표시
                if (viewModel.selectedPhotoIndex.value == null) {
                    updateMemo(day.dayMemo, "오늘의 메모")
                }
            }
        }

        // 사진 선택 시 메모 변경 관찰
        viewModel.selectedPhotoIndex.observe(viewLifecycleOwner) { idx ->
            val day = viewModel.selectedDay.value
            if (day != null) {
                if (idx == null) {
                    updateMemo(day.dayMemo, "오늘의 메모")
                } else {
                    val photoMemo = day.photos.getOrNull(idx)?.memo
                    updateMemo(photoMemo ?: day.dayMemo, "사진 메모")
                }
                photoAdapter.setSelectedIndex(idx)
            }
        }
    }

    private fun updateMemo(content: String, title: String) {
        tvMemoTitle.text = title
        tvMemoContent.text = content
    }

    private fun setupWindowInsets(view: View) {
        val rootView = view.findViewById<View>(R.id.root) ?: return
        val initialPaddingTop = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, initialPaddingTop + statusBarTop, v.paddingRight, v.paddingBottom)
            insets
        }
    }
}