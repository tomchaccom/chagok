package com.example.myapplication.feature.present

import android.os.Bundle
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

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPresentBinding {
        return FragmentPresentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPresentData()
    }

    private fun setupRecyclerView() {
        // 에러가 났던 practicesRecyclerView 대신 rvTodayGoals 사용
        todayGoalAdapter = TodayGoalAdapter { goal, isAchieved ->
            // ViewModel의 기존 함수 이름에 맞춰 호출 (onPracticeStateChanged 등)
            viewModel.onPracticeStateChanged(goal.id, isAchieved)
        }

        binding.rvTodayGoals.apply {
            adapter = todayGoalAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        // 기록하기 버튼
        binding.btnRecordNow.setOnClickListener {
            navigateToCreateMoment()
        }

        // 차곡이 이미지 클릭 시
        binding.ivChagokHappy.setOnClickListener {
            navigateToCreateMoment()
        }
    }

    private fun navigateToCreateMoment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, CreateMomentFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.apply {
                        // 1. 인사말 및 남은 개수
                        tvUserWelcome.text = uiState.userProfile.greeting
                        tvGoalCount.text = "${uiState.practicesLeft}개 남음"

                        // 2. 리스트 갱신 (ViewModel의 practices 데이터를 TodayGoal 형태로 변환이 필요할 수 있음)
                        // 임시로 직접 대입하거나, 변환 로직 추가
                        // todayGoalAdapter.submitList(uiState.practices)
                    }
                }
            }
        }
    }
}