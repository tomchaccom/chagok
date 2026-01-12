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
import com.example.myapplication.R
import com.example.myapplication.data.past.PastRepository
import com.example.myapplication.feature.past.DayAdapter
import com.example.myapplication.feature.past.PhotoAdapter

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

        // ViewModel: repository 주입용 factory 사용 (없으면 Hilt 또는 기본 생성자 사용)
        val repo = PastRepository(requireContext())
        val factory = PastViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(PastViewModel::class.java)

        // Day list
        dayAdapter = DayAdapter { day ->
            viewModel.selectDay(day)
            showDetailFor(day)
        }
        rvDays.layoutManager = LinearLayoutManager(requireContext())
        rvDays.adapter = dayAdapter

        // Photo grid
        photoAdapter = PhotoAdapter { position ->
            viewModel.togglePhoto(position)
        }
        rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)
        rvPhotos.adapter = photoAdapter

        btnBack.setOnClickListener {
            viewModel.clearDay()
            showList()
        }

        // Observe
        viewModel.days.observe(viewLifecycleOwner) { days ->
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
                    tvMemoTitle.text = "사진 메모"
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
        tvMemoTitle.text = "사진 메모"
        tvMemoContent.text = day.dayMemo
        photoAdapter.submitList(day.photos)
    }
}
