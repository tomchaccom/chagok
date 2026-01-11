package com.example.myapplication.feature.present

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
    private val recordListViewModel: RecordListViewModel by viewModels()
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
        observeRecords()
        observeRecordEvents()
        observeLoadingState()
        observeErrorState()
    }

    override fun onResume() {
        super.onResume()
        // Fragment가 다시 보일 때마다 버튼 보이기
        binding.createMomentButton.visibility = View.VISIBLE
        binding.addRecordIcon.visibility = View.VISIBLE
    }

    private fun setupRecyclerViews() {
        practiceAdapter = PracticeAdapter { practice, isAchieved ->
            viewModel.onPracticeStateChanged(practice.id, isAchieved)
        }
        binding.practicesRecyclerView.adapter = practiceAdapter

        recordAdapter = RecordAdapter { record, shouldSelect ->
            recordListViewModel.onMainImageToggleIntent(record.id, shouldSelect)
        }
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

                        // 기록 목록은 별도 스트림에서 관리합니다.
                    }
                }
            }
        }
    }

    private fun observeRecords() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recordListViewModel.records.collect { records ->
                    val hasRecords = records.isNotEmpty()
                    binding.emptyRecordCard.isVisible = !hasRecords
                    binding.recordsCarousel.isVisible = hasRecords
                    binding.recordsIndicator.isVisible = hasRecords
                    if (hasRecords) {
                        recordAdapter.submitList(records)
                    } else {
                        recordAdapter.submitList(emptyList())
                    }
                }
            }
        }
    }

    private fun observeRecordEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recordListViewModel.events.collect { event ->
                    when (event) {
                        RecordListViewModel.UiEvent.ShowMainImageReplaceDialog -> {
                            showMainImageReplaceDialog()
                        }
                    }
                }
            }
        }
    }

    private fun showMainImageReplaceDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage("A main image is already selected. Do you want to replace it?")
            .setPositiveButton("Replace") { _, _ ->
                recordListViewModel.confirmReplaceMainImage()
            }
            .setNegativeButton("Cancel") { _, _ ->
                recordListViewModel.cancelReplaceMainImage()
            }
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
