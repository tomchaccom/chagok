package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.databinding.FragmentExampleBinding

class ExampleFragment : BaseFragment<FragmentExampleBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentExampleBinding {
        return FragmentExampleBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.exampleText.setOnClickListener {
            showToast("Fragment Clicked!")
        }
    }
}