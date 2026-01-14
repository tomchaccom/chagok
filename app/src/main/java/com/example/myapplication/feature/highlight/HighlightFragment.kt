package com.example.myapplication.feature.highlight

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.databinding.FragmentHighlightBinding
import com.example.myapplication.databinding.ViewHighlightRankItemBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

class HighlightFragment : BaseFragment<FragmentHighlightBinding>() {

    private val viewModel: HighlightViewModel by activityViewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHighlightBinding {
        return FragmentHighlightBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeSelector()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshIfNeeded()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 1. 데이터 유무에 따른 가이드 텍스트 처리
                    if (state.showEmptyState) {
                        binding.explanationTitle.text = "기록을 쌓아주세요"
                        binding.explanationBody.text = "차곡이가 당신의 기록을 분석할 준비를 하고 있어요!"
                    } else {
                        // 현재 선택된 토글 버튼에 따라 UI 업데이트
                        updateUiBySelectedMetric(state)
                    }
                }
            }
        }
    }

    /*private fun setupThemeSelector() {
        // 초기값 설정 (나다움 버튼)
        binding.themeToggleGroup.check(R.id.theme_identity_button)

        binding.themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                // 버튼 클릭 시 UI 강제 갱신을 위해 viewModel에 알리거나 로컬 UI 즉시 변경
                // 여기서는 현재 수집 중인 state를 기반으로 다시 그림
                viewLifecycleOwner.lifecycleScope.launch {
                    updateUiBySelectedMetric(viewModel.uiState.value)
                }
            }
        }
    }*/

    /*private fun updateUiBySelectedMetric(state: HighlightUiState) {
        val selectedMetric = when (binding.themeToggleGroup.checkedButtonId) {
            R.id.theme_identity_button -> HighlightMetric.IDENTITY
            R.id.theme_connectivity_button -> HighlightMetric.CONNECTIVITY
            R.id.theme_perspective_button -> HighlightMetric.PERSPECTIVE
            else -> HighlightMetric.IDENTITY
        }

        val section = state.sections.find { it.metric == selectedMetric } ?: return

        // 1. 설명 텍스트 업데이트
        binding.explanationTitle.text = section.metric.title
        binding.explanationBody.text = getMetricDescription(section.metric)

        // 2. 랭킹 바인딩 (Top 3)
        bindMomentRank(binding.layoutRank1, section.items.getOrNull(0), "1")
        bindMomentRank(binding.layoutRank2, section.items.getOrNull(1), "2")
        bindMomentRank(binding.layoutRank3, section.items.getOrNull(2), "3")

        // 3. 그래프 바인딩
        if (section.canShowGraph) {
            bindLineChart(section)
        }
    }
*/
    private fun bindMomentRank(
        rankBinding: ViewHighlightRankItemBinding,
        item: HighlightRankItem?,
        rank: String
    ) {
        if (item == null) {
            rankBinding.root.visibility = View.GONE
            return
        }
        rankBinding.root.visibility = View.VISIBLE
        rankBinding.tvRankBadge.text = rank
        rankBinding.tvRankMemo.text = item.memo
        rankBinding.tvRankDate.text = "2026.01.13" // 실제 데이터 date 연결 권장
        rankBinding.tvRankScore.text = "${item.score}점"

        // 1위는 강조 컬러
        val color = if (rank == "1") "#4CAF50" else "#888888"
        rankBinding.tvRankBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
    }

    private fun getMetricDescription(metric: HighlightMetric): String {
        return when (metric) {
            HighlightMetric.IDENTITY -> "이 지표는 기록이 당신의 정체성에 얼마나 깊이 뿌리내렸는지 보여줘요."
            HighlightMetric.CONNECTIVITY -> "이 지표는 기록이 타인 또는 세상과 얼마나 연결되어 있는지 보여줘요."
            HighlightMetric.PERSPECTIVE -> "이 지표는 기록이 당신의 생각이나 관점을 얼마나 확장시켰는지 보여줘요."
        }
    }

    private fun bindLineChart(section: HighlightRankSection) {
        val points = section.graphPoints
        val chart = LineChart(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        binding.chartContainer.removeAllViews()
        binding.chartContainer.addView(chart)

        val entries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.value.toFloat()) }
        val dataSet = LineDataSet(entries, "").apply {
            color = Color.parseColor("#4CAF50")
            lineWidth = 3f
            setDrawValues(false)
            setDrawCircles(true)
            setCircleColor(Color.parseColor("#4CAF50"))
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(points.map { it.label })
            animateX(500)
            invalidate()
        }
    }

    private fun setupThemeSelector() {
        // 1. 초기 선택값 설정
        binding.themeToggleGroup.check(R.id.theme_identity_button)

        // 2. 버튼 클릭 리스너
        binding.themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.theme_ai_button -> {
                        // [AI 모드]
                        binding.standardContentLayout.visibility = View.GONE
                        binding.aiResultCard.visibility = View.VISIBLE

                        val itemsToAnalyze = viewModel.uiState.value.sections.flatMap { it.items }
                        viewModel.fetchAiAnalysis(itemsToAnalyze)
                    }
                    else -> {
                        // [일반 지표 모드]
                        binding.standardContentLayout.visibility = View.VISIBLE
                        binding.aiResultCard.visibility = View.GONE

                        // 🌟 1. 차곡이 메시지 즉시 업데이트
                        updateMetricMessages(checkedId)

                        // 🌟 2. 리스트 및 그래프 업데이트
                        updateUiBySelectedMetric(viewModel.uiState.value)
                    }
                }
            }
        }
    }
    private fun updateMetricMessages(checkedId: Int) {
        when (checkedId) {
            R.id.theme_identity_button -> {
                binding.explanationTitle.text = "나다운 기억 (Identity)"
                binding.explanationBody.text = "이 지표는 기록이 당신의 정체성에 얼마나 깊이 뿌리내렸는지 보여줘요."
            }
            R.id.theme_connectivity_button -> {
                binding.explanationTitle.text = "연결된 기억 (Connectivity)"
                binding.explanationBody.text = "이 지표는 기록이 타인 또는 세상과 얼마나 연결되어 있는지 보여줘요."
            }
            R.id.theme_perspective_button -> {
                binding.explanationTitle.text = "새로운 관점 (Perspective)"
                binding.explanationBody.text = "이 지표는 기록이 당신의 생각이나 관점을 얼마나 확장시켰는지 보여줘요."
            }
        }
    }




    private fun updateUiBySelectedMetric(state: HighlightUiState) {
        // AI 버튼이 눌린 상태라면 업데이트를 건너뜀 (이미 위에서 GONE 처리함)
        if (binding.themeToggleGroup.checkedButtonId == R.id.theme_ai_button) return

        val selectedMetric = when (binding.themeToggleGroup.checkedButtonId) {
            R.id.theme_identity_button -> HighlightMetric.IDENTITY
            R.id.theme_connectivity_button -> HighlightMetric.CONNECTIVITY
            R.id.theme_perspective_button -> HighlightMetric.PERSPECTIVE
            else -> HighlightMetric.IDENTITY
        }

        val section = state.sections.find { it.metric == selectedMetric } ?: return

        // 1. 설명 텍스트 업데이트 (여기서 차곡이 오른쪽 메시지가 바뀝니다!)
        binding.explanationTitle.text = section.metric.title
        binding.explanationBody.text = getMetricDescription(section.metric)

        // 2. 랭킹 바인딩
        bindMomentRank(binding.layoutRank1, section.items.getOrNull(0), "1")
        bindMomentRank(binding.layoutRank2, section.items.getOrNull(1), "2")
        bindMomentRank(binding.layoutRank3, section.items.getOrNull(2), "3")

        // 3. 그래프 바인딩
        if (section.canShowGraph) {
            bindLineChart(section)
        }
    }

    // 🌟 AI 응답을 받아서 화면에 그려주는 함수 추가
    private fun observeAiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.aiState.collect { state ->
                    when (state) {
                        is AiUiState.Loading -> {
                            binding.aiLoadingView.visibility = View.VISIBLE
                            binding.tvAiContent.text = ""
                        }
                        is AiUiState.Success -> {
                            binding.aiLoadingView.visibility = View.GONE
                            binding.tvAiContent.text = state.message
                        }
                        is AiUiState.Error -> {
                            binding.aiLoadingView.visibility = View.GONE
                            binding.tvAiContent.text = "오류 발생: ${state.error}"
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}