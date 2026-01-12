package com.example.myapplication.feature.present

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.core.base.BaseFragment
import com.example.myapplication.core.util.ImageUtils
import com.example.myapplication.databinding.FragmentCreateMomentBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * CreateMomentFragment
 *
 * 기능:
 * - 사진 선택 (갤러리 / 카메라)
 * - 메모 입력
 * - CES 지수 입력 (Identity, Connectivity, Perspective)
 * - 대표 기억 체크
 * - 저장 후 PresentFragment로 복귀
 */
class CreateMomentFragment : BaseFragment<FragmentCreateMomentBinding>() {

    private val viewModel: CreateMomentViewModel by viewModels()
    private var currentPhotoFile: File? = null
    private var featuredCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    /* ---------------- 권한 & 런처 ---------------- */

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                showToast("카메라 권한이 필요합니다")
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleSelectedPhoto(uri)
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK && currentPhotoFile?.exists() == true) {
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    currentPhotoFile!!
                )
                handleSelectedPhoto(photoUri)
            } else {
                showToast("사진 촬영이 취소되었습니다")
            }
        }

    /* ---------------- Fragment 기본 ---------------- */

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateMomentBinding {
        return FragmentCreateMomentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_EDIT_RECORD_ID)?.let { recordId ->
            viewModel.startEdit(recordId)
        }

        setupToolbar()
        setupCesSliders() // CES 슬라이더 설정으로 변경
        setupPhotoButtons()
        setupMemoInput()
        setupFeaturedCheckbox()
        setupSaveButton()
        observeUiState()
    }

    /* ---------------- UI 세팅 ---------------- */

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * CES(Identity, Connectivity, Perspective) 슬라이더 초기화
     */
    private fun setupCesSliders() {
        // 1. Identity Slider
        binding.identitySlider.apply {
            valueFrom = 1f
            valueTo = 5f
            stepSize = 1f
            addOnChangeListener { _, value, _ ->
                binding.identityValue.text = value.toInt().toString()
                viewModel.setCesIdentity(value.toInt())
            }
        }

        // 2. Connectivity Slider
        binding.connectivitySlider.apply {
            valueFrom = 1f
            valueTo = 5f
            stepSize = 1f
            addOnChangeListener { _, value, _ ->
                binding.connectivityValue.text = value.toInt().toString()
                viewModel.setCesConnectivity(value.toInt())
            }
        }

        // 3. Perspective Slider
        binding.perspectiveSlider.apply {
            valueFrom = 1f
            valueTo = 5f
            stepSize = 1f
            addOnChangeListener { _, value, _ ->
                binding.perspectiveValue.text = value.toInt().toString()
                viewModel.setCesPerspective(value.toInt())
            }
        }
    }

    private fun setupPhotoButtons() {
        binding.changePhotoButton.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }

        binding.cameraButton.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
    }

    private fun setupMemoInput() {
        binding.memoEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setMemo(s.toString())
            }
        })
    }

    private fun setupFeaturedCheckbox() {
        featuredCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            viewModel.setFeatured(isChecked)
        }
        binding.featuredCheckbox.setOnCheckedChangeListener(featuredCheckedChangeListener)
    }

    private fun setupSaveButton() {
        binding.saveMomentButton.setOnClickListener {
            val state = viewModel.uiState.value

            if (state.selectedPhotoUri.isNullOrBlank()) {
                showToast("사진을 선택해주세요")
                return@setOnClickListener
            }

            viewModel.saveMoment()
        }
    }

    /* ---------------- Camera ---------------- */

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (intent.resolveActivity(requireActivity().packageManager) == null) {
            showToast("카메라 앱이 없습니다")
            return
        }

        currentPhotoFile = createImageFile()

        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            currentPhotoFile!!
        )

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JPEG_${timeStamp}_"
        val storageDir = requireContext().externalCacheDir
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    private fun handleSelectedPhoto(uri: Uri) {
        val correctedBitmap = ImageUtils.fixImageOrientation(requireContext(), uri)
        val correctedUri = correctedBitmap?.let { saveBitmapToCache(it) } ?: uri
        viewModel.setSelectedPhoto(correctedUri.toString())
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(requireContext().externalCacheDir, "IMG_${timeStamp}.jpg")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }

    /* ---------------- State 관찰 ---------------- */

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // 1. 사진 미리보기
                    if (!state.selectedPhotoUri.isNullOrBlank()) {
                        binding.photoPreview.setImageURI(state.selectedPhotoUri.toUri())
                        binding.photoPlaceholder.isVisible = false
                    } else {
                        binding.photoPlaceholder.isVisible = true
                    }

                    // 2. CES 값 및 총점 업데이트
                    binding.identityValue.text = state.cesInput.identity.toString()
                    binding.connectivityValue.text = state.cesInput.connectivity.toString()
                    binding.perspectiveValue.text = state.cesInput.perspective.toString()

                    if (binding.memoEditText.text?.toString() != state.memo) {
                        binding.memoEditText.setText(state.memo)
                        binding.memoEditText.setSelection(state.memo.length)
                    }

                    binding.cesScoreValue.text = "${state.cesWeightedScore}점"
                    binding.cesScoreDescription.text = state.cesDescription

                    // 슬라이더 값 동기화 (ViewModel 상태와 UI 일치)
                    if (binding.identitySlider.value != state.cesInput.identity.toFloat()) {
                        binding.identitySlider.value = state.cesInput.identity.toFloat()
                    }
                    if (binding.connectivitySlider.value != state.cesInput.connectivity.toFloat()) {
                        binding.connectivitySlider.value = state.cesInput.connectivity.toFloat()
                    }
                    if (binding.perspectiveSlider.value != state.cesInput.perspective.toFloat()) {
                        binding.perspectiveSlider.value = state.cesInput.perspective.toFloat()
                    }

                    // 3. 버튼 상태
                    val enabled = !state.isLoading
                    binding.changePhotoButton.isEnabled = enabled
                    binding.cameraButton.isEnabled = enabled
                    binding.saveMomentButton.isEnabled = enabled

                    // 4. 에러 처리
                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearErrorMessage()
                    }

                    // 5. 대표 기억 체크박스
                    if (binding.featuredCheckbox.isChecked != state.isFeatured) {
                        binding.featuredCheckbox.setOnCheckedChangeListener(null)
                        binding.featuredCheckbox.isChecked = state.isFeatured
                        binding.featuredCheckbox.setOnCheckedChangeListener(featuredCheckedChangeListener)
                    }

                    // 6. 대표 기억 충돌 다이얼로그
                    if (state.showFeaturedConflictDialog) {
                        viewModel.consumeFeaturedConflictDialog()
                        showFeaturedConflictDialog()
                    }

                    // 7. 저장 완료
                    if (state.savedSuccessfully) {
                        showToast("순간이 저장되었습니다")
                        viewModel.resetSavedState()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    private fun showFeaturedConflictDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("이미 대표 기억이 설정되어 있습니다.\n현재 이미지를 대표 기억으로 변경하시겠습니까?")
            .setPositiveButton("변경") { _, _ ->
                viewModel.confirmFeaturedReplacement(true)
            }
            .setNegativeButton("취소") { _, _ ->
                viewModel.confirmFeaturedReplacement(false)
            }
            .show()
    }

    companion object {
        private const val ARG_EDIT_RECORD_ID = "arg_edit_record_id"

        fun newInstance(recordId: String): CreateMomentFragment {
            return CreateMomentFragment().apply {
                arguments = bundleOf(ARG_EDIT_RECORD_ID to recordId)
            }
        }
    }
}
