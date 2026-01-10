package com.example.myapplication.feature.present

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.databinding.FragmentPresentBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class PresentFragment : BaseFragment<FragmentPresentBinding>() {

    private val viewModel: PresentViewModel by activityViewModels { PresentViewModelFactory() }
    private lateinit var practiceAdapter: PracticeAdapter
    private lateinit var recordAdapter: RecordAdapter

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPresentBinding {
        return FragmentPresentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        observeUiState()
        observeLoadingState()
        observeErrorState()
    }

    override fun onResume() {
        super.onResume()
        // Fragment가 다시 보일 때마다 버튼 보이기
        binding.createMomentButton.visibility = View.VISIBLE
        binding.addRecordIcon.visibility = View.VISIBLE

        // CreateMomentFragment에서 돌아올 때 데이터 갱신
        refreshRecordsList()
    }

    private fun refreshRecordsList() {
        // CreateMomentViewModel에서 저장된 기록들을 다시 로드하여 RecordAdapter 갱신
        val savedRecords = CreateMomentViewModel.getSavedRecords()
        if (savedRecords.isNotEmpty()) {
            binding.emptyRecordCard.visibility = View.GONE
            binding.recordsCarousel.visibility = View.VISIBLE
            binding.recordsIndicator.visibility = View.VISIBLE
            recordAdapter.submitList(savedRecords.toList())
        }
    }

    private fun setupRecyclerViews() {
        practiceAdapter = PracticeAdapter { practice, isAchieved ->
            viewModel.onPracticeStateChanged(practice.id, isAchieved)
        }
        binding.practicesRecyclerView.adapter = practiceAdapter

        recordAdapter = RecordAdapter()
        binding.recordsCarousel.adapter = recordAdapter

        // Link the TabLayout and ViewPager2 for the indicator
        TabLayoutMediator(binding.recordsIndicator, binding.recordsCarousel) { tab, position ->
            // No-op, we only need the dots
        }.attach()
    }

    private fun setupClickListeners() {
        binding.createMomentButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, CreateMomentFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.addRecordIcon.setOnClickListener {
            AddPracticeModal().show(childFragmentManager, AddPracticeModal.TAG)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.apply {
                        greetingText.text = uiState.userProfile.greeting
                        greetingPrompt.text = uiState.userProfile.prompt
                        practicesLeftBadge.text = "${uiState.practicesLeft}개 남음"
                        practiceAdapter.submitList(uiState.practices)

                        // CreateMomentViewModel에서 저장된 기록들을 가져옴
                        val savedRecords = CreateMomentViewModel.getSavedRecords()
                        val hasRecords = savedRecords.isNotEmpty()
                        emptyRecordCard.isVisible = !hasRecords
                        recordsCarousel.isVisible = hasRecords
                        recordsIndicator.isVisible = hasRecords

                        if (hasRecords) {
                            recordAdapter.submitList(savedRecords)
                        }
                    }
                }
            }
        }
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

    private fun observeErrorState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { errorMessage ->
                    errorMessage?.let {
                        showToast(it)
                        viewModel.clearErrorMessage()
                    }
                }
            }
        }
    }
}