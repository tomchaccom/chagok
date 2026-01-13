package com.example.myapplication.feature.present

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
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.data.future.GoalRepository
import com.example.myapplication.data.present.PracticeRepository
import com.example.myapplication.databinding.FragmentPresentBinding
import kotlinx.coroutines.launch
import java.time.LocalDate

class PresentFragment : BaseFragment<FragmentPresentBinding>() {

    private val viewModel: PresentViewModel by activityViewModels { PresentViewModelFactory() }

    private lateinit var practiceAdapter: PracticeAdapter
    private lateinit var recordAdapter: RecordAdapter // MomentAdapter에서 이름 변경

    private var practiceAdapterObserver: RecyclerView.AdapterDataObserver? = null
    private val localOverrides: MutableMap<String, Boolean?> = mutableMapOf()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPresentBinding {
        return FragmentPresentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 리서이클러뷰 초기 세팅
        setupRecyclerViews()

        // 2. 로컬 저장소 데이터 초기화
        initLocalRepositories()

        // 3. 리스너 및 관찰자 설정
        setupClickListeners()
        observeUiState()
        observeLoadingState()
        observeErrorState()
    }

    private fun initLocalRepositories() {
        PracticeRepository.initialize(requireContext())
        try {
            val saved = PracticeRepository.load()
            if (saved.isNotEmpty()) localOverrides.putAll(saved)
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        viewModel.loadPresentData()
        // 메모리에 저장된 레코드 업데이트
        val savedRecords = CreateMomentViewModel.getSavedRecords()
        updateRecordUi(savedRecords)
        loadTodayPracticesFromGoals()
    }

    private fun setupRecyclerViews() {
        // --- [오늘의 실천 섹션] ---
        practiceAdapter = PracticeAdapter { practice, isAchieved ->
            localOverrides[practice.id] = isAchieved
            val current = practiceAdapter.currentList.toMutableList()
            val idx = current.indexOfFirst { it.id == practice.id }
            if (idx >= 0) {
                current[idx] = current[idx].copy(isAchieved = isAchieved)
                practiceAdapter.submitList(current) { updateGoalCountBadge() }
            }
            viewModel.onPracticeStateChanged(practice.id, isAchieved)
            try { PracticeRepository.save(localOverrides) } catch (_: Exception) {}
        }

        binding.rvTodayGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = practiceAdapter
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
        // "이 순간 기록하기" 버튼
        binding.btnRecordNow.setOnClickListener { navigateToCreateMoment() }
        // 차곡이 캐릭터 이미지 클릭 시에도 기록하기로 이동
        binding.ivChagokHappy.setOnClickListener { navigateToCreateMoment() }
    }

    private fun updateRecordUi(records: List<DailyRecord>) {
        val sortedRecords = records.reversed()
        recordAdapter.submitList(sortedRecords)

        val hasRecords = sortedRecords.isNotEmpty()
        binding.apply {
            // 기록이 있으면 리스트를 보여주고, 없으면 안내 카드(emptyStateLayout)를 보여줌
            recordRecyclerView.isVisible = hasRecords
            emptyStateLayout.isVisible = !hasRecords
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.apply {
                        tvUserWelcome.text = uiState.userProfile.greeting
                        // Practice 리스트 병합 및 제출
                        val merged = mergeWithLocal(uiState.practices)
                        practiceAdapter.submitList(merged) { updateGoalCountBadge() }
                    }
                }
            }
        }
    }

    private fun updateGoalCountBadge() {
        val remainingCount = practiceAdapter.currentList.count { it.isAchieved != true }
        binding.tvGoalCount.text = "${remainingCount}개 남음"
    }

    private fun mergeWithLocal(remote: List<Practice>): List<Practice> {
        return remote.map { practice ->
            practice.copy(isAchieved = localOverrides[practice.id] ?: practice.isAchieved)
        }
    }

    private fun loadTodayPracticesFromGoals() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                GoalRepository.initialize(requireContext())
                val todayGoals = GoalRepository.getAll()
                    .filter { it.date == LocalDate.now() }
                    .map { Practice("goal-${it.title.hashCode()}", it.title, "오늘의 목표", null) }
                practiceAdapter.submitList(mergeWithLocal(todayGoals)) { updateGoalCountBadge() }
            } catch (_: Exception) {}
        }
    }

    // --- 네비게이션 로직 ---
    private fun navigateToCreateMoment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, CreateMomentFragment()) // MainActivity의 컨테이너 ID 확인 필요
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

    // private fun observeLoadingState() { /* 로딩 구현 */ }
    private fun observeErrorState() { /* 에러 구현 */ }
    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // override 하지 말고, 부모 클래스(BaseFragment)에 이미 정의된
                    // showLoading 함수를 호출만 하면 됩니다.
                    showLoading(isLoading)
                }
            }
        }
    }

// ❌ 이 아래에 있던 override fun showLoading(...) { ... } 블록은 삭제하세요.
// 부모 클래스에 이미 구현되어 있다면 중복으로 만들 필요가 없습니다.
}