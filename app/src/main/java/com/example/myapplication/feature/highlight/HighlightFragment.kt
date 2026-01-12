package com.example.myapplication.feature.highlight

import android.content.res.Resources
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
import com.example.myapplication.databinding.ItemHighlightSectionBinding
import com.example.myapplication.feature.present.CreateMomentViewModel
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
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        val records = CreateMomentViewModel.getSavedRecords()
        viewModel.refreshIfNeeded(records)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val sections = state.sections
                    val hasSections = sections.isNotEmpty()
                    binding.highlightEmptyCard.isVisible = !hasSections

                    bindSection(
                        binding.sectionMasterpiece,
                        sections.firstOrNull { it.type == HighlightType.MASTERPIECE }
                    )
                    bindSection(
                        binding.sectionHiddenDriver,
                        sections.firstOrNull { it.type == HighlightType.HIDDEN_DRIVER }
                    )
                    bindSection(
                        binding.sectionEmotionalAnchor,
                        sections.firstOrNull { it.type == HighlightType.EMOTIONAL_ANCHOR }
                    )

                    emphasizeTopScore(sections)
                }
            }
        }
    }

    private fun bindSection(
        sectionBinding: ItemHighlightSectionBinding,
        section: HighlightSection?
    ) {
        val primary = section?.primary
        sectionBinding.root.isVisible = primary != null
        if (primary == null || section == null) {
            return
        }

        sectionBinding.highlightTitle.text = section.title
        sectionBinding.highlightDescription.text = section.description
        sectionBinding.highlightMemo.text =
            if (primary.memo.isNotBlank()) primary.memo else "메모가 없습니다."
        sectionBinding.highlightScoreValue.text =
            "${primary.identityScore} / ${primary.connectivityScore} / ${primary.perspectiveScore}"

        if (primary.photoUri.isNotBlank()) {
            sectionBinding.highlightPhoto.setImageURI(android.net.Uri.parse(primary.photoUri))
        } else {
            sectionBinding.highlightPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        sectionBinding.highlightCard.setOnClickListener {
            navigateToPresent(primary.recordId)
        }

        // TODO: secondary 후보를 보여주는 디자인과 상호작용을 추가하세요.
        val accentColor = when (section.type) {
            HighlightType.MASTERPIECE -> R.color.primary
            HighlightType.HIDDEN_DRIVER -> R.color.secondary_accent
            HighlightType.EMOTIONAL_ANCHOR -> R.color.error
        }
        sectionBinding.highlightAccent.setBackgroundColor(requireContext().getColor(accentColor))

        val totalScore = primary.identityScore + primary.connectivityScore + primary.perspectiveScore
        sectionBinding.highlightCard.tag = totalScore
        applyPhotoHeight(sectionBinding, totalScore)
    }

    private fun emphasizeTopScore(sections: List<HighlightSection>) {
        val primaryItems = sections.mapNotNull { it.primary }
        val topScore = primaryItems.maxOfOrNull {
            it.identityScore + it.connectivityScore + it.perspectiveScore
        } ?: return

        val bindings = listOf(
            binding.sectionMasterpiece,
            binding.sectionHiddenDriver,
            binding.sectionEmotionalAnchor
        )
        for (sectionBinding in bindings) {
            val totalScore = sectionBinding.highlightCard.tag as? Int
            val isTop = totalScore == topScore
            sectionBinding.highlightCard.strokeWidth =
                if (isTop) dpToPx(2) else dpToPx(1)
            sectionBinding.highlightCard.cardElevation =
                if (isTop) dpToPx(6).toFloat() else dpToPx(4).toFloat()
        }
    }

    private fun applyPhotoHeight(
        sectionBinding: ItemHighlightSectionBinding,
        score: Int
    ) {
        val minHeight = 160
        val maxHeight = 240
        val normalized = ((score - 3).coerceIn(0, 12)).toFloat() / 12f
        val heightDp = (minHeight + (maxHeight - minHeight) * normalized).toInt()
        val params = sectionBinding.highlightPhoto.layoutParams
        params.height = dpToPx(heightDp)
        sectionBinding.highlightPhoto.layoutParams = params
    }

    private fun navigateToPresent(recordId: String) {
        val bottomNavigation = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottomNavigation
        )
        bottomNavigation.selectedItemId = R.id.navigation_present
        // TODO: PresentFragment와 연결하여 recordId로 스크롤 이동을 지원하세요.
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
}
