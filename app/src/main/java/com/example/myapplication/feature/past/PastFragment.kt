package com.example.myapplication.feature.past

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.past.PastRepository
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.data.present.DailyRecord
import com.example.myapplication.feature.present.CreateMomentViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 뷰 초기화 및 시스템 바 인셋 설정
        rvDays = view.findViewById(R.id.rvDays)
        setupWindowInsets(view)

        // 2. Repository 및 데이터 임포트 (데이터를 먼저 파일에 저장해야 ViewModel이 읽을 수 있음)
        val repo = PastRepository(requireContext())
        // importPresentToPastBeforeVm(repo)

        // 3. ViewModel 설정 (Shared ViewModel)
        val factory = PastViewModelFactory(repo)
        viewModel = ViewModelProvider(requireActivity(), factory).get(PastViewModel::class.java)

        // 4. 어댑터 설정 (클릭 시 상세 이동 로직 포함)
        setupRecyclerView()

        // 5. 데이터 관찰 (Observer)
        viewModel.days.observe(viewLifecycleOwner) { days ->
            dayAdapter.submitList(null)
            dayAdapter.submitList(days)
        }

        // 6. 데이터 로드 명령 (관찰을 시작한 직후에 호출하여 즉시 화면 갱신)
        viewModel.loadDays()
    }

    private fun setupRecyclerView() {
        dayAdapter = DayAdapter { day ->
            // 상세 화면으로 데이터 전달 및 이동
            viewModel.selectDay(day)

            val detailFragment = PastDetailFragment()
            parentFragmentManager.beginTransaction().apply {
                setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                replace(R.id.container, detailFragment)
                addToBackStack(null)
                commit()
            }
        }
        rvDays.layoutManager = LinearLayoutManager(requireContext())
        rvDays.adapter = dayAdapter
    }

    private fun importPresentToPastBeforeVm(repo: PastRepository) {
        try {
            val saved = CreateMomentViewModel.getSavedRecords()
            if (saved.isEmpty()) return

            val groups = saved.groupBy { rec ->
                if (rec.date.isBlank()) currentDateIso() else rec.date
            }

            for ((dateIso, records) in groups) {
                val dateLabel = formatDateLabel(dateIso)
                val dayPhotos = records.reversed()

                // 중복 날짜 체크 기능이 있는 Repository의 함수 호출
                val newDay = DayEntry(id = 0L, dateLabel = dateLabel, photos = dayPhotos)
                repo.addOrUpdateDayEntry(newDay)
            }

            // 임포트 완료 후 현재 세션 비우기
            CreateMomentViewModel.clearRecords()
            showToast("오늘의 기억이 과거 카드에 통합되었습니다.")
        } catch (_: Exception) { }
    }

    private fun setupWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun currentDateIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return fmt.format(Date())
    }

    private fun formatDateLabel(dateStr: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = inFmt.parse(dateStr) ?: return dateStr
            val outFmt = SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault())
            outFmt.format(d)
        } catch (_: Exception) {
            dateStr
        }
    }
    override fun onResume() {
        super.onResume()
        // 화면으로 다시 돌아올 때마다 최신 데이터를 불러옵니다.
        if (::viewModel.isInitialized) {
            viewModel.loadDays()
        }
    }
}