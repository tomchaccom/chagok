package com.example.myapplication.feature.highlight

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
                    // 1. 차곡이 메시지 및 가이드 업데이트
                    if (state.showEmptyState) {
                        binding.tvHighlightGuide.text = "아직 기록이 없어요. 사진기 든 차곡이와 함께 첫 기록을 남겨볼까요?"
                        binding.tvEmptyChartMessage.visibility = View.VISIBLE
                    } else {
                        binding.tvHighlightGuide.text = "기록을 살펴보니 당신의 삶은 이 지표들을 중심으로 움직이고 있네요!"
                        binding.tvEmptyChartMessage.visibility = View.GONE
                    }

                    // 2. 랭킹 데이터 바인딩
                    bindRankings(state.sections)

                    // 3. 통합 그래프 바인딩 (대표 섹션 혹은 전체 평균 그래프)
                    state.sections.firstOrNull { it.canShowGraph }?.let { representativeSection ->
                        bindLineChart(representativeSection)
                    }
                }
            }
        }
    }

    private fun bindRankings(sections: List<HighlightRankSection>) {
        // 점수 순으로 정렬 (내림차순)
        val sortedSections = sections.sortedByDescending { it.averageScore }

        // 1위: Identity (예시 매칭 - 실제 데이터에 따라 title 매칭)
        val rank1 = sortedSections.getOrNull(0)
        bindRankItem(binding.layoutRankIdentity, rank1, "1", true)

        // 2위: Connectivity
        val rank2 = sortedSections.getOrNull(1)
        bindRankItem(binding.layoutRankConnectivity, rank2, "2", false)

        // 3위: Perspective
        val rank3 = sortedSections.getOrNull(2)
        bindRankItem(binding.layoutRankPerspective, rank3, "3", false)
    }

    private fun bindRankItem(
        rankBinding: ViewHighlightRankItemBinding,
        section: HighlightRankSection?,
        rank: String,
        isFirst: Boolean
    ) {
        rankBinding.root.visibility = if (section != null) View.VISIBLE else View.GONE
        section?.let {
            rankBinding.tvRankBadge.text = rank
            rankBinding.tvRankTitle.text = it.metric.title
            rankBinding.tvRankScore.text = String.format("%.1f점", it.averageScore)

            // 1등은 초록색 배지, 나머지는 회색
            val badgeColor = if (isFirst) "#4CAF50" else "#888888"
            rankBinding.tvRankBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(badgeColor))
        }
    }

    private fun bindLineChart(section: HighlightRankSection) {
        val points = section.graphPoints
        if (points.isEmpty()) return

        // 꺾은선 그래프용 커스텀 차트 생성 (차트 뷰 동적 추가 또는 FrameLayout 활용)
        val chart = LineChart(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        binding.chartContainer.removeAllViews()
        binding.chartContainer.addView(chart)

        val entries = points.mapIndexed { index, point ->
            Entry(index.toFloat(), point.value.toFloat())
        }

        val dataSet = LineDataSet(entries, section.metric.title).apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER // 부드러운 곡선
            setDrawFilled(true)
            fillColor = Color.parseColor("#4CAF50")
            fillAlpha = 30
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(points.map { it.label })
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 5.5f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
            }
            animateXY(800, 800)
            invalidate()
        }
    }
}