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
        identityAdapter = HighlightRankAdapter { item ->
            navigateToPresent(item.recordId)
        }
        connectivityAdapter = HighlightRankAdapter { item ->
            navigateToPresent(item.recordId)
        }
        perspectiveAdapter = HighlightRankAdapter { item ->
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

    private fun bindSections(sections: List<HighlightRankSection>) {
        val identitySection = sections.firstOrNull { it.metric == HighlightMetric.IDENTITY }
        val connectivitySection = sections.firstOrNull { it.metric == HighlightMetric.CONNECTIVITY }
        val perspectiveSection = sections.firstOrNull { it.metric == HighlightMetric.PERSPECTIVE }

        bindSection(binding.sectionIdentity, identitySection, identityAdapter)
        bindSection(binding.sectionConnectivity, connectivitySection, connectivityAdapter)
        bindSection(binding.sectionPerspective, perspectiveSection, perspectiveAdapter)
    }

    private fun bindSection(
        sectionBinding: com.example.myapplication.databinding.ItemHighlightSectionBinding,
        section: HighlightRankSection?,
        adapter: HighlightRankAdapter
    ) {
        sectionBinding.root.isVisible = section != null && section.items.isNotEmpty()
        if (section == null) {
            adapter.submitList(emptyList())
            return
        }

        sectionBinding.sectionTitle.text = section.metric.title
        adapter.submitList(section.items)
    }

    private fun navigateToPresent(recordId: String) {
        val bottomNavigation = requireActivity()
            .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottomNavigation
            )
        bottomNavigation.selectedItemId = R.id.navigation_present
        // TODO: PresentFragment와 연결하여 recordId로 스크롤 이동을 지원하세요.
    }
}
