package com.example.myapplication.feature.highlight

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        observeAiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshIfNeeded()
    }

    /**
     * AI 분석 결과 관찰
     * 🌟 해결책: 타입 체크 후 'as' 키워드나 스마트 캐스트를 위해 명확한 경로를 지정합니다.
     */
    private fun observeAiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.aiState.collect { state ->
                    when (state) {
                        is AiUiState.Loading -> {
                            updateAiVisibility(loading = true)
                        }
                        is AiUiState.Success -> {
                            updateAiVisibility(success = true)
                            // 스마트 캐스트가 동작하여 message에 접근 가능합니다.
                            binding.tvAiContent.text = state.message
                        }
                        is AiUiState.Error -> {
                            updateAiVisibility(ready = true)
                            // 스마트 캐스트가 동작하여 error에 접근 가능합니다.
                            binding.tvAiContent.text = "오류가 발생했습니다: ${state.error}"
                            Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            updateAiVisibility(ready = true)
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

                        val currentAiState = viewModel.aiState.value
                        if (currentAiState is AiUiState.Success) {
                            updateAiVisibility(success = true)
                            binding.tvAiContent.text = currentAiState.message
                        } else {
                            updateAiVisibility(ready = true)
                        }
                    }
                    else -> {
                        binding.standardContentLayout.visibility = View.VISIBLE
                        binding.aiResultCard.visibility = View.GONE
                        updateUiBySelectedMetric(viewModel.uiState.value)
                    }
                }
            }
        }

        binding.btnStartAiAnalysis.setOnClickListener {
            updateAiVisibility(loading = true)
            val itemsToAnalyze = viewModel.uiState.value.sections.flatMap { it.items }
            if (itemsToAnalyze.isNotEmpty()) {
                viewModel.fetchAiAnalysis(itemsToAnalyze)
            } else {
                updateAiVisibility(ready = true)
                Toast.makeText(requireContext(), "분석할 데이터가 부족합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.showEmptyState) {
                        binding.explanationTitle.text = "기록을 더 쌓아주세요"
                        binding.explanationBody.text = "최소 3개의 기록이 있어야 통계와 AI 분석이 가능해요."
                        binding.standardContentLayout.visibility = View.GONE
                        binding.aiResultCard.visibility = View.GONE
                    } else {
                        if (binding.themeToggleGroup.checkedButtonId == R.id.theme_ai_button) {
                            binding.standardContentLayout.visibility = View.GONE
                            binding.aiResultCard.visibility = View.VISIBLE
                        } else {
                            binding.standardContentLayout.visibility = View.VISIBLE
                            binding.aiResultCard.visibility = View.GONE
                            updateUiBySelectedMetric(state)
                        }
                    }
                }
            }
        }
    }

    private fun updateAiVisibility(ready: Boolean = false, loading: Boolean = false, success: Boolean = false) {
        binding.apply {
            layoutAiReady.visibility = if (ready) View.VISIBLE else View.GONE
            aiLoadingView.visibility = if (loading) View.VISIBLE else View.GONE
            ivAiChagok.visibility = if (success) View.VISIBLE else View.GONE
            tvAiContent.visibility = if (success) View.VISIBLE else View.GONE
        }
    }

    private fun updateUiBySelectedMetric(state: HighlightUiState) {
        val selectedId = binding.themeToggleGroup.checkedButtonId
        if (selectedId == R.id.theme_ai_button) return

        val selectedMetric = when (selectedId) {
            R.id.theme_identity_button -> HighlightMetric.IDENTITY
            R.id.theme_connectivity_button -> HighlightMetric.CONNECTIVITY
            R.id.theme_perspective_button -> HighlightMetric.PERSPECTIVE
            else -> HighlightMetric.IDENTITY
        }

        val section = state.sections.find { it.metric == selectedMetric } ?: return
        binding.explanationTitle.text = section.metric.title
        binding.explanationBody.text = getMetricDescription(section.metric)

        bindMomentRank(binding.layoutRank1, section.items.getOrNull(0), "1")
        bindMomentRank(binding.layoutRank2, section.items.getOrNull(1), "2")
        bindMomentRank(binding.layoutRank3, section.items.getOrNull(2), "3")

        if (section.canShowGraph) bindLineChart(section)
    }

    private fun bindMomentRank(rankBinding: ViewHighlightRankItemBinding, item: HighlightRankItem?, rank: String) {
        if (item == null) { rankBinding.root.visibility = View.GONE; return }
        rankBinding.root.visibility = View.VISIBLE
        rankBinding.tvRankBadge.text = rank
        rankBinding.tvRankMemo.text = item.memo
        rankBinding.tvRankDate.text = "2026.01.14"
        rankBinding.tvRankScore.text = "${item.score}점"
        val color = if (rank == "1") "#4CAF50" else "#888888"
        rankBinding.tvRankBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
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
            data = LineData(dataSet); description.isEnabled = false; legend.isEnabled = false; axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM; xAxis.valueFormatter = IndexAxisValueFormatter(points.map { it.label })
            animateX(500); invalidate()
        }
    }

    private fun getMetricDescription(metric: HighlightMetric): String {
        return when (metric) {
            HighlightMetric.IDENTITY -> "이 지표는 기록이 당신의 정체성에 얼마나 깊이 뿌리내렸는지 보여줘요."
            HighlightMetric.CONNECTIVITY -> "이 지표는 기록이 타인 또는 세상과 얼마나 연결되어 있는지 보여줘요."
            HighlightMetric.PERSPECTIVE -> "이 지표는 기록이 당신의 생각이나 관점을 얼마나 확장시켰는지 보여줘요."
        }
    }
}