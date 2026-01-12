package com.example.myapplication.feature.highlight

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
import com.example.myapplication.databinding.FragmentHighlightBinding
import kotlinx.coroutines.launch
import com.example.myapplication.databinding.ItemHighlightSectionBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter



class HighlightFragment : BaseFragment<FragmentHighlightBinding>() {

    private val viewModel: HighlightViewModel by activityViewModels()
    private lateinit var identityAdapter: HighlightRankAdapter
    private lateinit var connectivityAdapter: HighlightRankAdapter
    private lateinit var perspectiveAdapter: HighlightRankAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHighlightBinding {
        return FragmentHighlightBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
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
                    binding.highlightEmptyCard.isVisible = state.showEmptyState
                    bindSections(state.sections)
                }
            }
        }
    }

    private fun setupAdapters() {
        identityAdapter = HighlightRankAdapter(
            getString(R.string.highlight_metric_identity_badge)
        ) { item ->
            navigateToPresent(item.recordId)
        }
        connectivityAdapter = HighlightRankAdapter(
            getString(R.string.highlight_metric_connectivity_badge)
        ) { item ->
            navigateToPresent(item.recordId)
        }
        perspectiveAdapter = HighlightRankAdapter(
            getString(R.string.highlight_metric_perspective_badge)
        ) { item ->
            navigateToPresent(item.recordId)
        }

        binding.sectionIdentity.sectionList.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.sectionIdentity.sectionList.adapter = identityAdapter
        binding.sectionConnectivity.sectionList.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.sectionConnectivity.sectionList.adapter = connectivityAdapter
        binding.sectionPerspective.sectionList.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.sectionPerspective.sectionList.adapter = perspectiveAdapter
    }

    private fun setupThemeSelector() {
        binding.themeToggleGroup.check(binding.themeIdentityButton.id)
        updateTheme(HighlightMetric.IDENTITY)

        binding.themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
            val metric = when (checkedId) {
                binding.themeIdentityButton.id -> HighlightMetric.IDENTITY
                binding.themeConnectivityButton.id -> HighlightMetric.CONNECTIVITY
                binding.themePerspectiveButton.id -> HighlightMetric.PERSPECTIVE
                else -> HighlightMetric.IDENTITY
            }
            updateTheme(metric)
        }
    }

    private fun bindSections(sections: List<HighlightRankSection>) {
        val identitySection = sections.firstOrNull { it.metric == HighlightMetric.IDENTITY }
        val connectivitySection = sections.firstOrNull { it.metric == HighlightMetric.CONNECTIVITY }
        val perspectiveSection = sections.firstOrNull { it.metric == HighlightMetric.PERSPECTIVE }

        bindSection(binding.sectionIdentity, identitySection, identityAdapter)
        bindSection(binding.sectionConnectivity, connectivitySection, connectivityAdapter)
        bindSection(binding.sectionPerspective, perspectiveSection, perspectiveAdapter)
    }

    private fun updateTheme(metric: HighlightMetric) {
        val explanation = when (metric) {
            HighlightMetric.IDENTITY -> ThemeExplanation(
                letter = R.string.highlight_metric_letter_identity,
                title = R.string.highlight_theme_identity_title,
                body = R.string.highlight_theme_identity_body,
                guide = 0
            )
            HighlightMetric.CONNECTIVITY -> ThemeExplanation(
                letter = R.string.highlight_metric_letter_connectivity,
                title = R.string.highlight_theme_connectivity_title,
                body = R.string.highlight_theme_connectivity_body,
                guide = 0
            )
            HighlightMetric.PERSPECTIVE -> ThemeExplanation(
                letter = R.string.highlight_metric_letter_perspective,
                title = R.string.highlight_theme_perspective_title,
                body = R.string.highlight_theme_perspective_body,
                guide = 0
            )
        }

        binding.explanationLetter.setText(explanation.letter)
        binding.explanationTitle.setText(explanation.title)
        binding.explanationBody.setText(explanation.body)
        if (explanation.guide == 0) {
            binding.explanationGuide.isVisible = false
        } else {
            binding.explanationGuide.isVisible = true
            binding.explanationGuide.setText(explanation.guide)
        }

        binding.sectionIdentity.root.isVisible = metric == HighlightMetric.IDENTITY
        binding.sectionConnectivity.root.isVisible = metric == HighlightMetric.CONNECTIVITY
        binding.sectionPerspective.root.isVisible = metric == HighlightMetric.PERSPECTIVE
    }

    private fun bindSection(
        sectionBinding: ItemHighlightSectionBinding,
        section: HighlightRankSection?,
        adapter: HighlightRankAdapter
    ) {
        if (section == null) {
            adapter.submitList(emptyList())
            sectionBinding.sectionGraphContainer.isVisible = false
            sectionBinding.sectionGraphEmpty.isVisible = true
            return
        }

        sectionBinding.sectionTitle.text = section.metric.title
        adapter.submitList(section.items)

        sectionBinding.sectionGraphContainer.isVisible = section.canShowGraph
        sectionBinding.sectionGraphEmpty.isVisible = !section.canShowGraph

        if (section.canShowGraph) {
            bindBarChart(
                chart = sectionBinding.sectionGraphChart,
                points = section.graphPoints
            )
        }
    }



    private fun navigateToPresent(recordId: String) {
        val bottomNavigation = requireActivity()
            .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottomNavigation
            )
        bottomNavigation.selectedItemId = R.id.navigation_present
        // TODO: PresentFragment와 연결하여 recordId로 스크롤 이동을 지원하세요.
    }

    private data class ThemeExplanation(
        val letter: Int,
        val title: Int,
        val body: Int,
        val guide: Int
    )

    companion object {
        private const val MIN_GRAPH_POINTS = 3
    }


    private fun bindBarChart(
        chart: BarChart,
        points: List<HighlightGraphPoint>
    ) {
        val entries = points.mapIndexed { index, point ->
            BarEntry(index.toFloat(), point.value.toFloat())
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = requireContext().getColor(R.color.primary)
            valueTextSize = 10f
            setDrawValues(true)
        }

        chart.data = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            setDrawGridBackground(false)
            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(
                    points.map { it.label }
                )
            }

            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
            }

            animateY(500)
            invalidate()
        }
    }

}
