package com.example.myapplication.feature.present

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.myapplication.databinding.FragmentAddPracticeBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddPracticeModal : BottomSheetDialogFragment() {

    private var _binding: FragmentAddPracticeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PresentViewModel by activityViewModels {
        PresentViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPracticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addButton.setOnClickListener {
            val practiceText = binding.practiceEditText.text.toString()
            if (practiceText.isNotBlank()) {
                viewModel.onAddPractice(practiceText)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddPracticeModal"
    }
}