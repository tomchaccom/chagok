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

import com.example.myapplication.data.present.Meaning as DataMeaning

// 🌟 데이터 충돌 방지를 위한 Alias(별칭) 설정
import com.example.myapplication.data.future.Goal as DataGoal
import com.example.myapplication.data.present.DailyRecord as DataDailyRecord // 추가: DailyRecord도 별칭 사용
import com.example.myapplication.feature.present.DailyRecord as FeatureDailyRecord // 불필요시 삭제 가능

class PresentFragment : BaseFragment<FragmentPresentBinding>() {

    private val viewModel: PresentViewModel by activityViewModels { PresentViewModelFactory() }

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
        // 🌟 CreateMomentViewModel에서 가져온 레코드를 DataDailyRecord 타입 리스트로 인식
        val savedRecords: List<DataDailyRecord> = CreateMomentViewModel.getSavedRecords()
        updateRecordUi(savedRecords)
        loadTodayPracticesFromGoals()
    }

    private fun setupRecyclerViews() {
        todayGoalAdapter = TodayGoalAdapter { goal ->
            navigateToCreateMoment(goal)
        }

        binding.rvTodayGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todayGoalAdapter
        }

        // 🌟 RecordAdapter 내부의 리스트 타입도 DataDailyRecord여야 합니다.
        recordAdapter = RecordAdapter { record ->
            showEditMomentDialog(record)
        }
        binding.recordRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recordAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnRecordNow.setOnClickListener { navigateToCreateMoment(null) }
        binding.ivChagokHappy.setOnClickListener { navigateToCreateMoment(null) }
    }

    // 🌟 파라미터 타입을 별칭(DataDailyRecord)으로 변경
    private fun updateRecordUi(records: List<DataDailyRecord>) {
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
                }
            }
        }
    }

    private fun loadTodayPracticesFromGoals() {
        try {
            GoalRepository.initialize(requireContext())

            val todayDataGoals = GoalRepository.getAll()
                .filter { it.date == LocalDate.now() }
                .map { item ->
                    DataGoal(
                        id = item.id,
                        title = item.title,
                        date = item.date,
                        isAchieved = item.isAchieved
                    )
                }

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

    private fun navigateToCreateMoment(goal: DataGoal?) {
        val fragment = CreateMomentFragment().apply {
            arguments = Bundle().apply {
                goal?.let {
                    putString("GOAL_TITLE", it.title)
                    putString("GOAL_ID", it.id)
                }
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // 🌟 파라미터 타입을 DataDailyRecord로 변경
    private fun showEditMomentDialog(record: DataDailyRecord) {
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