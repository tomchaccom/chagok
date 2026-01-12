package com.example.myapplication.feature.present

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
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.databinding.FragmentPresentBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import kotlin.collections.reversed

class PresentFragment : BaseFragment<FragmentPresentBinding>() {

    // PresentViewModel은 기존대로 유지 (Practice 관련 데이터 관리)
    private val viewModel: PresentViewModel by activityViewModels { PresentViewModelFactory() }

    // 어댑터 정의
    private lateinit var practiceAdapter: PracticeAdapter
    private lateinit var momentAdapter: MomentAdapter // RecordAdapter -> MomentAdapter로 변경

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
        // 화면이 다시 보일 때마다 UI 상태 갱신 (저장하고 돌아왔을 때 반영)
        refreshData()
    }

    private fun refreshData() {
        // 1. Practice 데이터 갱신 (ViewModel)
        viewModel.loadPresentData()

        // 2. Record 데이터 갱신 (CreateMomentViewModel의 Static 데이터 사용)
        // 주의: 실제 앱에서는 Room DB나 서버 데이터를 PresentViewModel에서 불러오는 것이 좋습니다.
        val savedRecords = CreateMomentViewModel.getSavedRecords()
        updateRecordUi(savedRecords)
    }

    private fun setupRecyclerViews() {
        // 1. Practice RecyclerView (기존 유지)
        practiceAdapter = PracticeAdapter { practice, isAchieved ->
            viewModel.onPracticeStateChanged(practice.id, isAchieved)
        }
        binding.practicesRecyclerView.adapter = practiceAdapter

        // 2. Moment Carousel (ViewPager2) - 변경됨
        momentAdapter = MomentAdapter { record ->
            showEditMomentDialog(record)
        }
        binding.recordsCarousel.apply {
            adapter = momentAdapter
            offscreenPageLimit = 1 // 양옆의 카드를 미리 로드하여 부드럽게
            // (선택사항) 양옆 카드 살짝 보이게 하려면 padding과 clipToPadding 설정이 XML에 있어야 함
        }

        // 3. Indicator 연결 (TabLayout + ViewPager2)
        TabLayoutMediator(binding.recordsIndicator, binding.recordsCarousel) { tab, position ->
            // 탭 텍스트 없이 점만 표시
        }.attach()
    }

    private fun setupClickListeners() {
        // "이 순간 기록하기" 버튼
        binding.createMomentButton.setOnClickListener {
            navigateToCreateMoment()
        }

        // 빈 화면 카드 내의 "+" 아이콘
        // (기존 코드에서는 AddPracticeModal이었으나, UI상 '기록'이 없을 때 뜨는 카드이므로 기록 화면으로 이동이 자연스러움)
        binding.addRecordIcon.setOnClickListener {
            navigateToCreateMoment()
        }
    }

    private fun navigateToCreateMoment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, CreateMomentFragment()) // container ID 확인 필요
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToEditMoment(record: DailyRecord) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, CreateMomentFragment.newInstance(record.id)) // container ID 확인 필요
            .addToBackStack(null)
            .commit()
    }

    private fun showEditMomentDialog(record: DailyRecord) {
        if (!record.isToday()) {
            return
        }

        AlertDialog.Builder(requireContext())
            .setMessage("오늘의 기억을 수정하시겠습니까?\n오늘만 수정이 가능합니다.")
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("수정") { _, _ ->
                navigateToEditMoment(record)
            }
            .show()
    }

    private fun DailyRecord.isToday(): Boolean {
        if (date.isBlank()) {
            return false
        }
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        return date == today
    }


    private fun updateRecordUi(records: List<DailyRecord>) {
        // 최신순 정렬
        val sortedRecords = records.reversed()

        // 어댑터에 데이터 제출
        momentAdapter.submitList(sortedRecords)

        val hasRecords = sortedRecords.isNotEmpty()

        binding.apply {
            // 기록이 있으면 -> 캐러셀 보이기, 빈 카드 숨기기
            // (hasRecords가 Boolean으로 정상 인식되므로 ! 연산자 오류도 사라집니다)
            recordsCarousel.isVisible = hasRecords
            recordsIndicator.isVisible = hasRecords
            emptyRecordCard.isVisible = !hasRecords

            // 버튼은 항상 보이게 (또는 기획에 따라 조정)
            createMomentButton.isVisible = true
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

                        // Practice 리스트 갱신
                        practiceAdapter.submitList(uiState.practices)
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
