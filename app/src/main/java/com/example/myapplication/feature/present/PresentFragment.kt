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
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.databinding.FragmentPresentBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import kotlin.collections.reversed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.future.GoalRepository
import com.example.myapplication.data.present.PracticeRepository
import java.time.LocalDate
import android.widget.TextView

class PresentFragment : BaseFragment<FragmentPresentBinding>() {

    // PresentViewModel은 기존대로 유지 (Practice 관련 데이터 관리)
    private val viewModel: PresentViewModel by activityViewModels { PresentViewModelFactory() }

    // 어댑터 정의
    private lateinit var practiceAdapter: PracticeAdapter

    private lateinit var practices_left_badge: TextView

    private lateinit var momentAdapter: MomentAdapter // RecordAdapter -> MomentAdapter로 변경

    // AdapterDataObserver를 보관하여 onDestroyView에서 해제
    private var practiceAdapterObserver: RecyclerView.AdapterDataObserver? = null

    // 사용자가 토글한 실천 상태를 임시로 저장합니다 (로컬 우선 적용)
    private val localOverrides: MutableMap<String, Boolean?> = mutableMapOf()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPresentBinding {
        return FragmentPresentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // binding을 사용하여 뱃지 TextView를 먼저 초기화합니다. (어댑터 옵저버가 즉시 갱신할 수 있도록)
        practices_left_badge = binding.practicesLeftBadge

        setupRecyclerViews()

        // 저장된 present records 및 practice 상태 초기화
        try {
            CreateMomentViewModel.initialize(requireContext())
        } catch (_: Exception) {
        }
        PracticeRepository.initialize(requireContext())
        // 저장된 practice 상태를 로드하여 로컬 오버라이드에 적용
        try {
            val saved = PracticeRepository.load()
            if (saved.isNotEmpty()) {
                localOverrides.putAll(saved)
            }
        } catch (_: Exception) {
        }

        setupClickListeners()
        observeUiState()
        observeLoadingState()
        observeErrorState()

        // Present 화면이 보일 때 goals.json에 있는 오늘 목표를 로드하여 Practice로 표시
        loadTodayPracticesFromGoals()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Adapter observer 해제
        practiceAdapterObserver?.let {
            try {
                practiceAdapter.unregisterAdapterDataObserver(it)
            } catch (_: Exception) {
                // 이미 해제되었거나 adapter가 초기화되지 않았을 수 있음
            }
            practiceAdapterObserver = null
        }
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

        // goals에 변화가 있을 수 있으므로 다시 로드
        loadTodayPracticesFromGoals()
    }

    private fun setupRecyclerViews() {
        // 1. Practice RecyclerView (기존 유지)
        practiceAdapter = PracticeAdapter { practice, isAchieved ->
            // 즉시 UI 반영: 어댑터의 현재 리스트를 복사하여 해당 항목의 상태를 변경 후 다시 제출
            try {
                // 로컬 오버라이드에 우선 저장
                localOverrides[practice.id] = isAchieved

                val current = practiceAdapter.currentList.toMutableList()
                val idx = current.indexOfFirst { it.id == practice.id }
                if (idx >= 0) {
                    // data class라면 copy로 상태 변경
                    current[idx] = current[idx].copy(isAchieved = isAchieved)
                    // submitList의 commit 콜백으로 뱃지 갱신
                    practiceAdapter.submitList(current) {
                        updatePracticesLeftBadge(practiceAdapter.currentList)
                    }
                } else {
                    // 아이템이 없다면 어댑터 리스트를 강제로 갱신
                    practiceAdapter.submitList(current) {
                        updatePracticesLeftBadge(practiceAdapter.currentList)
                    }
                }
                // 변경된 토글 상태를 파일로 저장
                try {
                    PracticeRepository.save(localOverrides)
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
                // 안전하게 실패: adapter가 아직 준비 안 된 경우 등
            }

            // 변경을 ViewModel(또는 저장소)에 알려 영구 저장/동기화 처리
            viewModel.onPracticeStateChanged(practice.id, isAchieved)
        }
        // 명시적으로 LayoutManager 설정(가끔 XML 속성으로는 동작하지 않을 수 있음)
        binding.practicesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.practicesRecyclerView.adapter = practiceAdapter
        binding.practicesRecyclerView.visibility = View.VISIBLE

        // Adapter 데이터 변경을 감지하여 뱃지 갱신
        val observer = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                updatePracticesLeftBadge(practiceAdapter.currentList)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updatePracticesLeftBadge(practiceAdapter.currentList)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updatePracticesLeftBadge(practiceAdapter.currentList)
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                updatePracticesLeftBadge(practiceAdapter.currentList)
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                updatePracticesLeftBadge(practiceAdapter.currentList)
            }
        }
        practiceAdapter.registerAdapterDataObserver(observer)
        practiceAdapterObserver = observer

        // 초기 뱃지 상태 업데이트 (어댑터가 비어있지 않다면 즉시 반영)
        updatePracticesLeftBadge(practiceAdapter.currentList)

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
        TabLayoutMediator(binding.recordsIndicator, binding.recordsCarousel) { _, _ ->
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
        binding.imgChagokPresent.setOnClickListener {
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

                        // 받은 리스트와 로컬 리스트를 병합하여 로컬 변경 우선 반영
                        val merged = mergeWithLocal(uiState.practices)
                        practiceAdapter.submitList(merged) {
                            updatePracticesLeftBadge(practiceAdapter.currentList)
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

    // GoalRepository에서 오늘 날짜와 같은 Goal을 Practice로 변환하여 어댑터에 제출
    private fun loadTodayPracticesFromGoals() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                GoalRepository.initialize(requireContext())
                val today = LocalDate.now()
                val todaysGoals = GoalRepository.getAll()
                    .filter { it.date == today }
                    .map { goal ->
                        Practice(
                            id = "goal-${goal.title.hashCode()}",
                            title = goal.title,
                            subtitle = "목표",
                            isAchieved = null
                        )
                    }
                val merged = mergeWithLocal(todaysGoals)
                practiceAdapter.submitList(merged) {
                    // 실제 리스트가 어댑터에 적용된 직후 뱃지 갱신
                    updatePracticesLeftBadge(practiceAdapter.currentList)
                }

            }
        } catch (_: Exception) {
            // 안전하게 실패하면 아무 것도 하지 않음
        }
    }

    // remote(뷰모델/파일)에서 받은 리스트와 어댑터의 currentList를 병합합니다.
    // 로컬(어댑터)에 있는 항목이 동일 id를 갖는 경우 로컬 항목을 우선 사용합니다.
    private fun mergeWithLocal(remote: List<Practice>): List<Practice> {
        val local = practiceAdapter.currentList
        val localMap = local.associateBy { it.id }
        val remoteMap = remote.associateBy { it.id }

        val allIds = (remote.map { it.id } + local.map { it.id }).distinct()

        return allIds.mapNotNull { id ->
            // 1) 로컬 어댑터에 있는 항목 우선
            val base = localMap[id] ?: remoteMap[id]
            base?.let {
                // 2) 사용자가 토글한 값(localOverrides)이 있으면 적용
                if (localOverrides.containsKey(id)) {
                    it.copy(isAchieved = localOverrides[id])
                } else it
            }
        }
    }

    private fun updatePracticesLeftBadge(practices: List<Practice>) {
        // 남은 개수 계산: isAchieved == true 인 경우 완료로 간주
        val remainingCount = practices.count { it.isAchieved != true }.coerceAtLeast(0)
        // UI 스레드에서 안전하게 갱신
        practices_left_badge.post {
            practices_left_badge.text = resources.getString(R.string.practices_left, remainingCount)
        }
    }
}
