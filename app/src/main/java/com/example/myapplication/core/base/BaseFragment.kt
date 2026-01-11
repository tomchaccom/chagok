package com.example.myapplication.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * BaseFragment<VB : ViewBinding>
 * - ViewBinding을 제네릭으로 받아 사용합니다.
 * - binding은 onCreateView에서 초기화되고 onDestroyView에서 해제됩니다.
 *
 * 사용법:
 * class ExampleFragment : BaseFragment<FragmentExampleBinding>() {
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentExampleBinding {
 *         return FragmentExampleBinding.inflate(inflater, container, false)
 *     }
 * }
 *
 * 제공되는 헬퍼:
 * - showToast(message: String)
 * - showLoading(isVisible: Boolean) -> 내부적으로 BaseActivity 의 setLoadingVisibility 를 호출
 *
 * XML 컨벤션 (팀 규칙 — 코드 주석으로만 명시):
 * - 루트 레이아웃은 ConstraintLayout을 사용하세요.
 * - ViewBinding 변수명은 항상 `binding` 으로 사용하세요.
 * - ID 네이밍 규칙: tvTitle, btnConfirm, rvList, ivThumbnail 등 카멜케이스 및 접두사 사용.
 * - 로딩/empty/error UI는 필요 시 별도의 레이아웃 파일로 분리하세요 (예: layout_loading.xml)
 */
abstract class BaseFragment<VB : androidx.viewbinding.ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding
            ?: throw IllegalStateException("ViewBinding accessed outside of view lifecycle. Use binding only between onCreateView and onDestroyView.")

    /**
     * 각 프래그먼트는 ViewBinding을 inflate 하는 메서드를 구현해야 합니다.
     */
    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        // Inflate한 binding의 root는 null이 될 수 없으므로 안전하게 반환합니다.
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 간단한 토스트 헬퍼
     */
    protected fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 로딩 표시 요청
     * - BaseActivity 의 `setLoadingVisibility` 를 호출합니다.
     */
    protected fun showLoading(isVisible: Boolean) {
        (activity as? com.example.myapplication.core.base.BaseActivity)?.setLoadingVisibility(isVisible)
    }
}
