package com.example.myapplication.feature.highlight

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
                    // 🌟 무조건 탭 버튼 그룹은 보이게 설정 (기록 유무와 상관없이)
                    binding.themeToggleGroup.visibility = View.VISIBLE

                    if (state.showEmptyState) {
                        // 데이터가 3개 미만일 때: 가이드 메시지만 노출
                        binding.explanationTitle.text = "기록을 더 쌓아주세요"
                        binding.explanationBody.text = "최소 3개의 기록이 있어야 통계와 AI 분석이 가능해요."

                        binding.standardContentLayout.visibility = View.GONE
                        binding.aiResultCard.visibility = View.GONE
                    } else {
                        // 데이터가 3개 이상일 때: 현재 선택된 탭에 따라 화면 분기
                        if (binding.themeToggleGroup.checkedButtonId == R.id.theme_ai_button) {
                            binding.standardContentLayout.visibility = View.GONE
                            binding.aiResultCard.visibility = View.VISIBLE
                        } else {
                            binding.standardContentLayout.visibility = View.VISIBLE
                            binding.aiResultCard.visibility = View.GONE
                            updateUiBySelectedMetric(state) // 통계/리스트 업데이트
                        }
                    }
                }
            }
        }
    }

    private fun setupThemeSelector() {
        binding.themeToggleGroup.check(R.id.theme_identity_button)

        binding.themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.theme_ai_button -> {
                        binding.standardContentLayout.visibility = View.GONE
                        binding.aiResultCard.visibility = View.VISIBLE

                        // 🌟 에러 해결: records 대신 sections에서 데이터를 추출해서 전달
                        val itemsToAnalyze = viewModel.uiState.value.sections.flatMap { it.items }
                        viewModel.fetchAiAnalysis(itemsToAnalyze)
                    }
                    else -> {
                        binding.standardContentLayout.visibility = View.VISIBLE
                        binding.aiResultCard.visibility = View.GONE

                        updateMetricMessages(checkedId) // 차곡이 메시지 변경
                        updateUiBySelectedMetric(viewModel.uiState.value)
                    }
                }
            }
        }

        // AI 상태 관찰 함수 호출 (에러 해결을 위해 아래 함수도 확인)
        observeAiState()
    }
    private fun observeAiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.aiState.collect { state ->
                    // state의 타입을 명시적으로 체크합니다.
                    when (state) {
                        is AiUiState.Loading -> { // ViewModel 밖으로 꺼냈을 때
                            binding.aiLoadingView.visibility = View.VISIBLE
                            binding.tvAiContent.text = ""
                        }
                        is AiUiState.Success -> {
                            binding.aiLoadingView.visibility = View.GONE
                            binding.tvAiContent.text = state.message
                        }
                        is AiUiState.Error -> {
                            binding.aiLoadingView.visibility = View.GONE
                            binding.tvAiContent.text = state.error
                        }
                        else -> {
                            binding.aiLoadingView.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }


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

}