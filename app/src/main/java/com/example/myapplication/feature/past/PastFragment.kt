package com.example.myapplication.feature.past

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.past.PastRepository
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.data.present.DailyRecord as DataDailyRecord
import com.example.myapplication.feature.present.DailyRecord as FeatureDailyRecord
import android.widget.Toast

class PastFragment : Fragment() {

    private lateinit var viewModel: PastViewModel
    private lateinit var rvDays: androidx.recyclerview.widget.RecyclerView
    private lateinit var dayAdapter: DayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_past, container, false)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 뷰 초기화
        rvDays = view.findViewById(R.id.rvDays)

        // 2. 인셋 설정 (화면 여백) 함수 호출
        setupWindowInsets(view)

        // 3. ViewModel 설정
        val repo = PastRepository(requireContext())

        // Import any present saved records into repository BEFORE creating ViewModel
        importPresentToPastBeforeVm(repo)

        val factory = PastViewModelFactory(repo)

        // requireActivity()를 사용하여 DetailFragment와 동일한 ViewModel 인스턴스 공유
        viewModel = ViewModelProvider(requireActivity(), factory).get(PastViewModel::class.java)

        // 4. 어댑터 설정 (클릭 시 동작 정의)
        dayAdapter = DayAdapter { day ->
            // --- 여기가 클릭했을 때 실행되는 부분입니다 ---

            // (1) 선택된 날짜 저장
            viewModel.selectDay(day)

            // (2) 이동할 프래그먼트 생성
            val detailFragment = PastDetailFragment()

            // (3) 트랜잭션 시작 (화면 교체)
            parentFragmentManager.beginTransaction().apply {
                setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                // XML에서 찾은 'container' 영역을 detailFragment로 교체
                replace(R.id.container, detailFragment)
                // 뒤로가기 버튼을 위해 백스택 추가
                addToBackStack(null)
                commit()
            }
        }

        // 5. 리사이클러뷰 연결 (클릭 리스너 밖으로 빼야 화면에 목록이 보입니다!)
        rvDays.layoutManager = LinearLayoutManager(requireContext())
        rvDays.adapter = dayAdapter

        // 6. 데이터 관찰 (데이터가 변경되면 어댑터 업데이트)
        viewModel.days.observe(viewLifecycleOwner) { days ->
            dayAdapter.submitList(days)
        }

        // Fallback: ViewModel의 LiveData가 아직 발행되지 않았을 수 있으므로
        // 저장소에서 즉시 동기적으로 더미 데이터를 가져와 표시합니다.
        val current = viewModel.days.value
        if (current == null || current.isEmpty()) {
            try {
                val syncList = repo.loadPastEntries()
                if (syncList.isNotEmpty()) {
                    dayAdapter.submitList(syncList)
                }
            } catch (_: Exception) {
            }
        }


//        viewModel.selectedDay.observe(viewLifecycleOwner) { day ->
//            if (day == null) showList() else showDetailFor(day)
//        }
//
//        viewModel.selectedPhotoIndex.observe(viewLifecycleOwner) { idx ->
//            val day = viewModel.selectedDay.value
//            if (day == null) {
//                tvMemoTitle.text = ""
//                tvMemoContent.text = ""
//            } else {
//                val repMemo = day.representativePhoto?.memo ?: ""
//                if (idx == null) {
//                    tvMemoTitle.text = "오늘 메모"
//                    tvMemoContent.text = repMemo
//                } else {
//                    tvMemoTitle.text = "사진 메모"
//                    tvMemoContent.text = day.photos.getOrNull(idx)?.memo ?: repMemo
//                }
//            }
//            photoAdapter.setSelectedIndex(idx)
//        }
    }

    // 7. setupWindowInsets 함수 정의 (onViewCreated 바깥에 있어야 합니다)
    private fun setupWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun importPresentToPastBeforeVm(repo: PastRepository) {
        try {
            val saved = com.example.myapplication.feature.present.CreateMomentViewModel.getSavedRecords()

            if (saved.isEmpty()) return

            // no per-record logging in release; skip loop

            val groups = saved.groupBy { rec -> if (rec.date.isBlank()) currentDateIso() else rec.date }
            for ((dateIso, records) in groups) {
                val dateLabel = formatDateLabel(dateIso)
                val photos: List<DataDailyRecord> = records.reversed().map { mapToDataDaily(it) }
                val day = DayEntry(id = 0L, dateLabel = dateLabel, photos = photos)
                repo.addDayEntry(day)
            }

            // Clear present records immediately since we've imported them before VM creation
            com.example.myapplication.feature.present.CreateMomentViewModel.clearRecords()
            showToast("오늘의 기억이 '과거'에 저장되었습니다.")
         } catch (_: Exception) {
            // ignore
        }
    }

    private fun mapToDataDaily(r: FeatureDailyRecord): DataDailyRecord {
        return DataDailyRecord(
            id = r.id,
            photoUri = r.photoUri,
            memo = r.memo,
            score = r.score,
            cesMetrics = com.example.myapplication.data.present.CesMetrics(r.cesMetrics.identity, r.cesMetrics.connectivity, r.cesMetrics.perspective, r.cesMetrics.weightedScore),
            meaning = com.example.myapplication.data.present.Meaning.valueOf(r.meaning.name),
            date = r.date,
            isFeatured = r.isFeatured
        )
    }

    private fun currentDateIso(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return fmt.format(java.util.Date())
    }

    private fun formatDateLabel(dateStr: String): String {
        return try {
            val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val d = inFmt.parse(dateStr)
            if (d == null) return dateStr
            val outFmt = java.text.SimpleDateFormat("yyyy년 M월 d일", java.util.Locale.getDefault())
            outFmt.format(d)
        } catch (_: Exception) {
            dateStr
        }
    }
//
//    private fun showList() {
//        detailContainer.visibility = View.GONE
//        rvDays.visibility = View.VISIBLE
//    }


}
