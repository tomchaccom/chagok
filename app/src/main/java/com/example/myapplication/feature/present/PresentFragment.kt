package com.example.myapplication.feature.present

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.databinding.FragmentPresentBinding
import kotlinx.coroutines.launch

class PresentFragment : BaseFragment<FragmentPresentBinding>() {

    private val viewModel: PresentViewModel by activityViewModels { PresentViewModelFactory() }
    private lateinit var todayGoalAdapter: TodayGoalAdapter
    private lateinit var recordAdapter: RecordAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPresentBinding {
        return FragmentPresentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        // 1. 실천 목표 리스트 (Vertical)
        todayGoalAdapter = TodayGoalAdapter { goal, isAchieved ->
            viewModel.onPracticeStateChanged(goal.id, isAchieved)
        }
        binding.rvTodayGoals.apply {
            adapter = todayGoalAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // 2. 오늘의 기록 카드 뉴스 리스트 (Horizontal)
        recordAdapter = RecordAdapter { record ->
            navigateToEditMoment(record.id)
        }
        binding.recordRecyclerView.apply {
            adapter = recordAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    // 1. 로그 확인 (Logcat에서 'PresentFragment' 필터로 확인하세요)
                    Log.d("PresentFragment", "수신된 기록 개수: ${uiState.todayRecords.size}")

                    // 2. 인사말 및 목표 개수 반영
                    binding.tvUserWelcome.text = uiState.userProfile.greeting
                    binding.tvGoalCount.text = "${uiState.practicesLeft}개 남음"

                    // 3. 오늘의 실천(목표) 리스트 갱신
                    todayGoalAdapter.submitList(uiState.practices)

                    // 4. 데이터 유무에 따른 가시성 조절
                    val hasRecords = uiState.todayRecords.isNotEmpty()

                    // 기록이 있으면 RecyclerView를 보여주고, 없으면 emptyState(차곡이)를 보여줌
                    binding.recordRecyclerView.visibility = if (hasRecords) View.VISIBLE else View.GONE
                    binding.emptyStateLayout.visibility = if (hasRecords) View.GONE else View.VISIBLE

                    // 5. 오늘의 기록(카드 뉴스) 데이터 제출
                    if (hasRecords) {
                        recordAdapter.submitList(uiState.todayRecords)
                    }
                }
            }
        }
    }

    private fun navigateToEditMoment(recordId: String) {
        val fragment = CreateMomentFragment().apply {
            arguments = Bundle().apply { putString("RECORD_ID", recordId) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupClickListeners() {
        binding.btnRecordNow.setOnClickListener { navigateToCreateMoment() }
        binding.ivChagokHappy.setOnClickListener { navigateToCreateMoment() }
    }

    private fun navigateToCreateMoment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, CreateMomentFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPresentData()
    }

} // ✅ 클래스 끝