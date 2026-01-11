package com.example.myapplication.feature.past

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.databinding.FragmentSimplePlaceholderBinding

class PastFragment : BaseFragment<FragmentSimplePlaceholderBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSimplePlaceholderBinding {
        return FragmentSimplePlaceholderBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.placeholderText.setText(R.string.past_tab_placeholder)
    }
}
