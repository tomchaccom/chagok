package com.example.myapplication.feature.present

import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.data.future.GoalRepository
import com.example.myapplication.data.present.PracticeRepository
import com.example.myapplication.databinding.FragmentPresentBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
// 🌟 Alias 적용: 데이터 충돌 방지
import com.example.myapplication.data.future.Goal as DataGoal
import com.example.myapplication.feature.future.Goal as FeatureGoal

class PresentFragment : BaseFragment<FragmentPresentBinding>() {

    private val viewModel: PresentViewModel by activityViewModels { PresentViewModelFactory() }

    // 🌟 오늘 목표를 위한 전용 어댑터 사용
    private lateinit var todayGoalAdapter: TodayGoalAdapter
    private lateinit var recordAdapter: RecordAdapter

    private val localOverrides: MutableMap<String, Boolean?> = mutableMapOf()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPresentBinding {
        return FragmentPresentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLocalRepositories()
        setupRecyclerViews()
        setupClickListeners()
        observeUiState()
        observeLoadingState()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        viewModel.loadPresentData()
        val savedRecords = CreateMomentViewModel.getSavedRecords()
        updateRecordUi(savedRecords)
        loadTodayPracticesFromGoals()
    }

    private fun setupRecyclerViews() {
        // --- [오늘의 실천 섹션] ---
        // 🌟 binding.rvTodayGoals를 사용하여 참조 에러 해결
        todayGoalAdapter = TodayGoalAdapter { goal ->
            navigateToCreateMoment(goal)
        }

        binding.rvTodayGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayGoalAdapter
        }

        // --- [오늘의 기록 섹션] ---
        recordAdapter = RecordAdapter { record ->
            showEditMomentDialog(record)
        }
        binding.recordRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recordAdapter
        }
    }

    private fun setupClickListeners() {
        // "이 순간 기록하기" 버튼 및 캐릭터 이미지 클릭 시
        binding.btnRecordNow.setOnClickListener { navigateToCreateMoment(null) }
        binding.ivChagokHappy.setOnClickListener { navigateToCreateMoment(null) }
    }

    private fun updateRecordUi(records: List<DailyRecord>) {
        val sortedRecords = records.reversed()
        recordAdapter.submitList(sortedRecords)

        binding.apply {
            recordRecyclerView.isVisible = sortedRecords.isNotEmpty()
            emptyStateLayout.isVisible = sortedRecords.isEmpty()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.tvUserWelcome.text = uiState.userProfile.greeting
                    // 필요한 경우 여기서 리스트 업데이트 로직 추가
                }
            }
        }
    }


    // PresentFragment.kt 내부 loadTodayPracticesFromGoals 함수
    private fun loadTodayPracticesFromGoals() {
        try {
            // Repository 초기화 확인
            com.example.myapplication.data.future.GoalRepository.initialize(requireContext())

            val todayDataGoals = com.example.myapplication.data.future.GoalRepository.getAll()
                .filter { it.date == java.time.LocalDate.now() }
                .map { item ->
                    // 🌟 핵심 해결책: item의 원본 데이터를 그대로 복사합니다.
                    DataGoal(
                        id = item.id,            // 1. 고유 ID를 넘겨야 개별 인식이 가능합니다.
                        title = item.title,      // 2. 제목 유지
                        date = item.date,        // 3. 날짜 유지
                        isAchieved = item.isAchieved // 4. 🌟 저장된 실제 성취 여부를 그대로 반영합니다.
                    )
                }

            // 리스트 제출
            todayGoalAdapter.submitList(todayDataGoals) {
                updateGoalCountBadge()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateGoalCountBadge() {
        val remainingCount = todayGoalAdapter.currentList.count { !it.isAchieved }
        binding.tvGoalCount.text = "${remainingCount}개 남음"
    }

    // --- 네비게이션 로직 ---
    private fun navigateToCreateMoment(goal: DataGoal?) {
        val fragment = CreateMomentFragment().apply {
            arguments = Bundle().apply {
                // 🌟 목표가 있을 경우 제목을 넘겨줌
                 // 🌟 ID를 반드시 넘겨야 함
                goal?.let {
                    putString("GOAL_TITLE", it.title)
                    putString("GOAL_ID", it.id)}
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment) // 🌟 MainActivity의 FrameLayout ID 확인 필수
            .addToBackStack(null)
            .commit()
    }

    private fun showEditMomentDialog(record: DailyRecord) {
        AlertDialog.Builder(requireContext())
            .setMessage("오늘의 기억을 수정하시겠습니까?")
            .setPositiveButton("수정") { _, _ ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, CreateMomentFragment.newInstance(record.id))
                    .addToBackStack(null)
                    .commit()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    showLoading(isLoading)
                }
            }
        }
    }

    private fun initLocalRepositories() {
        PracticeRepository.initialize(requireContext())
        try {
            val saved = PracticeRepository.load()
            if (saved.isNotEmpty()) localOverrides.putAll(saved)
        } catch (_: Exception) {}
    }
}