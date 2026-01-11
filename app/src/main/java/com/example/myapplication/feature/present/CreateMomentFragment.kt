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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.R
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
 * - 점수 선택 (1~10)
 * - 대표 기억 체크
 * - 저장 후 PresentFragment로 복귀
 */
class CreateMomentFragment : BaseFragment<FragmentCreateMomentBinding>() {

    private val viewModel: CreateMomentViewModel by viewModels()
    private var currentPhotoFile: File? = null

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

        setupToolbar()
        setupSlider()
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

    private fun setupSlider() {
        binding.scoreSlider.valueFrom = 1f
        binding.scoreSlider.valueTo = 10f

        binding.scoreSlider.addOnChangeListener { _, value, _ ->
            val score = value.toInt()
            binding.scoreValue.text = score.toString()
            viewModel.setScore(score)
        }

        binding.scoreSlider.value = viewModel.uiState.value.score.toFloat()
        binding.scoreValue.text = viewModel.uiState.value.score.toString()
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
        // 한 줄 입력만 허용: 엔터/붙여넣기 시 줄바꿈 제거
        val noNewlineFilter = android.text.InputFilter { source, start, end, _, _, _ ->
            val sanitized = source?.subSequence(start, end)?.toString()
                ?.replace("\n", "")
                ?.replace("\r", "")
            sanitized
        }
        binding.memoEditText.filters = arrayOf(noNewlineFilter)
        binding.memoEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setMemo(s.toString())
            }
        })
    }

    private fun setupFeaturedCheckbox() {
        binding.featuredCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onFeaturedSelectionChanged(isChecked)
        }
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
        // EXIF 방향 정보를 반영한 Bitmap으로 보정 후 캐시에 저장합니다.
        val correctedBitmap = ImageUtils.fixImageOrientation(requireContext(), uri)
        val correctedUri = correctedBitmap?.let { saveBitmapToCache(it) } ?: uri
        viewModel.setSelectedPhoto(correctedUri.toString())
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        // 메모리에 저장할 때도 회전이 반영된 이미지를 사용하기 위해 캐시에 저장합니다.
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

                    // 사진 미리보기
                    if (!state.selectedPhotoUri.isNullOrBlank()) {
                        binding.photoPreview.setImageURI(state.selectedPhotoUri.toUri())
                        binding.photoPlaceholder.isVisible = false
                    } else {
                        binding.photoPlaceholder.isVisible = true
                    }

                    // 대표 기억 체크 상태 동기화 (사용자 취소 시 체크 해제)
                    binding.featuredCheckbox.setOnCheckedChangeListener(null)
                    binding.featuredCheckbox.isChecked = state.isFeatured
                    binding.featuredCheckbox.setOnCheckedChangeListener { _, isChecked ->
                        viewModel.onFeaturedSelectionChanged(isChecked)
                    }

                    // 버튼 상태
                    val enabled = !state.isLoading
                    binding.changePhotoButton.isEnabled = enabled
                    binding.cameraButton.isEnabled = enabled
                    binding.saveMomentButton.isEnabled = enabled

                    // 에러
                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearErrorMessage()
                    }

                    // 저장 완료
                    if (state.savedSuccessfully) {
                        showToast("순간이 저장되었습니다")
                        viewModel.resetSavedState()
                        parentFragmentManager.popBackStack()
                    }

                    if (state.showFeaturedReplaceDialog) {
                        showFeaturedReplaceDialog()
                    }
                }
            }
        }
    }

    private fun showFeaturedReplaceDialog() {
        viewModel.consumeFeaturedReplaceDialog()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setMessage("A main image is already selected. Do you want to replace it?")
            .setPositiveButton("Replace") { _, _ ->
                viewModel.confirmReplaceFeatured()
            }
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.cancelReplaceFeatured()
            }
            .show()
    }
}
