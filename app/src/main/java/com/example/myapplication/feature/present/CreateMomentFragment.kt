package com.example.myapplication.feature.present

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.databinding.FragmentCreateMomentBinding
import com.google.android.material.slider.Slider

class CreateMomentFragment : BaseFragment<FragmentCreateMomentBinding>() {

    private val viewModel: PresentViewModel by activityViewModels { PresentViewModelFactory() }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCreateMomentBinding {
        return FragmentCreateMomentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSlider()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupSlider() {
        binding.scoreSlider.addOnChangeListener { slider, value, fromUser ->
            binding.scoreValue.text = value.toInt().toString()
        }
        binding.scoreValue.text = binding.scoreSlider.value.toInt().toString() // Initial value
    }

    private fun setupClickListeners() {
        binding.changePhotoButton.setOnClickListener {
            // TODO: Implement image picker logic (e.g., using ActivityResultLauncher)
            showToast("사진 변경 기능 구현 필요")
        }

        binding.saveMomentButton.setOnClickListener {
            val memo = binding.memoEditText.text.toString()
            val score = binding.scoreSlider.value.toInt()
            // TODO: Get photo URI

            if (memo.isNotBlank()) {
                // viewModel.onSaveMoment(photoUri, memo, score)
                showToast("순간 저장 기능 구현 필요")
                parentFragmentManager.popBackStack()
            } else {
                showToast("메모를 입력해주세요.")
            }
        }
    }
}