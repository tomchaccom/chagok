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
import androidx.fragment.app.activityViewModels
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

class CreateMomentFragment : BaseFragment<FragmentCreateMomentBinding>() {

    companion object {
        private const val ARG_RECORD_ID = "arg_record_id"

        /**
         * 새로운 인스턴스를 생성하며 recordId를 전달합니다.
         */
        @JvmStatic
        fun newInstance(recordId: String) =
            CreateMomentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RECORD_ID, recordId)
                }
            }
    }

    // 1. 현재 화면의 입력 상태 관리 (독립적)
    private val createViewModel: CreateMomentViewModel by viewModels()

    // 2. 메인 화면 리스트 갱신용 (Activity 범위 공유)
    // Factory가 필요하다면 activityViewModels { PresentViewModelFactory() } 로 수정하세요.
    private val presentViewModel: PresentViewModel by activityViewModels()

    private var currentPhotoFile: File? = null
    private var featuredCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    /* ---------------- 권한 및 런처 설정 ---------------- */

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) launchCamera() else showToast("카메라 권한이 필요합니다")
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri -> handleSelectedPhoto(uri) }
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
            }
        }

    /* ---------------- Fragment 기본 설정 ---------------- */

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCreateMomentBinding {
        return FragmentCreateMomentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        initSliderTexts()
        setupCesSliders()
        setupPhotoButtons()
        setupMemoInput()
        setupFeaturedCheckbox()
        setupSaveButton()

        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun initSliderTexts() {
        binding.layoutIdentity.sliderTitle.text = "Identity (정체성)"
        binding.layoutIdentity.sliderSubTitle.text = "얼마나 '나'다운 기억이었나요?"
        binding.layoutConnectivity.sliderTitle.text = "Connectivity (연결성)"
        binding.layoutConnectivity.sliderSubTitle.text = "무의식적으로 떠오를 것 같은 기억인가요?"
        binding.layoutPerspective.sliderTitle.text = "Perspective (관점)"
        binding.layoutPerspective.sliderSubTitle.text = "이 기억이 앞날의 당신을 변화시킬 수 있나요?"
    }

    private fun setupCesSliders() {
        binding.layoutIdentity.sliderMain.addOnChangeListener { _, value, _ ->
            val msg = when(value.toInt()) {
                1, 2 -> "조금은 낯선 모습이었나요?"
                3 -> "평소의 당신다운 모습이네요."
                else -> "완벽하게 '나'다운 순간이었어요!"
            }
            updateChagok(msg, value)
            createViewModel.setCesIdentity(value.toInt())
        }

        binding.layoutConnectivity.sliderMain.addOnChangeListener { _, value, _ ->
            val msg = when(value.toInt()) {
                1, 2 -> "혼자만의 깊은 시간이었군요."
                3 -> "세상과 기분 좋게 연결된 느낌!"
                else -> "모든 것이 하나로 이어진 듯해요."
            }
            updateChagok(msg, value)
            createViewModel.setCesConnectivity(value.toInt())
        }

        binding.layoutPerspective.sliderMain.addOnChangeListener { _, value, _ ->
            val msg = when(value.toInt()) {
                1, 2 -> "익숙하고 편안한 시선이었어요."
                3 -> "새로운 생각을 해보게 되었네요."
                else -> "세상을 보는 눈이 한 뼘 더 커졌어요!"
            }
            updateChagok(msg, value)
            createViewModel.setCesPerspective(value.toInt())
        }
    }

    private fun updateChagok(message: String, value: Float) {
        binding.tvChagokMessage.text = message
        val scaleFactor = 1.1f + (value * 0.04f)
        binding.ivChagokEmo.animate().scaleX(scaleFactor).scaleY(scaleFactor).setDuration(150)
            .withEndAction { binding.ivChagokEmo.animate().scaleX(1.0f).scaleY(1.0f).start() }.start()
    }

    private fun setupPhotoButtons() {
        binding.changePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = "image/*" }
            galleryLauncher.launch(intent)
        }
        binding.cameraButton.setOnClickListener { checkCameraPermissionAndLaunch() }
    }

    private fun setupMemoInput() {
        binding.memoEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                createViewModel.setMemo(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupFeaturedCheckbox() {
        featuredCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            createViewModel.setFeatured(isChecked)
        }
        binding.featuredCheckbox.setOnCheckedChangeListener(featuredCheckedChangeListener)
    }

    private fun setupSaveButton() {
        binding.saveMomentButton.setOnClickListener {
            val state = createViewModel.uiState.value
            if (state.selectedPhotoUri.isNullOrBlank()) {
                showToast("사진을 선택해주세요")
                return@setOnClickListener
            }
            createViewModel.saveMoment()
        }
    }

    /* ---------------- 사진 처리 로직 ---------------- */

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        currentPhotoFile = createImageFile()
        val photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", currentPhotoFile!!)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", requireContext().externalCacheDir)
    }

    private fun handleSelectedPhoto(uri: Uri) {
        val correctedBitmap = ImageUtils.fixImageOrientation(requireContext(), uri)
        val correctedUri = correctedBitmap?.let { saveBitmapToCache(it) } ?: uri
        createViewModel.setSelectedPhoto(correctedUri.toString())
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(requireContext().externalCacheDir, "IMG_${timeStamp}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
    }

    /* ---------------- UI 상태 관찰 ---------------- */

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                createViewModel.uiState.collect { state ->
                    // 사진 미리보기
                    if (!state.selectedPhotoUri.isNullOrBlank()) {
                        binding.photoPreview.setImageURI(state.selectedPhotoUri.toUri())
                        binding.photoPlaceholder.visibility = View.GONE
                    } else {
                        binding.photoPlaceholder.visibility = View.VISIBLE
                    }

                    // CES 텍스트 업데이트
                    binding.layoutIdentity.sliderValueText.text = state.cesInput.identity.toString()
                    binding.layoutConnectivity.sliderValueText.text = state.cesInput.connectivity.toString()
                    binding.layoutPerspective.sliderValueText.text = state.cesInput.perspective.toString()
                    binding.cesScoreValue.text = "${state.cesWeightedScore}점"
                    binding.cesScoreDescription.text = state.cesDescription

                    // 슬라이더 값 동기화
                    if (binding.layoutIdentity.sliderMain.value != state.cesInput.identity.toFloat())
                        binding.layoutIdentity.sliderMain.value = state.cesInput.identity.toFloat()
                    if (binding.layoutConnectivity.sliderMain.value != state.cesInput.connectivity.toFloat())
                        binding.layoutConnectivity.sliderMain.value = state.cesInput.connectivity.toFloat()
                    if (binding.layoutPerspective.sliderMain.value != state.cesInput.perspective.toFloat())
                        binding.layoutPerspective.sliderMain.value = state.cesInput.perspective.toFloat()

                    // 저장 버튼 상태
                    binding.saveMomentButton.isEnabled = !state.isLoading

                    // 체크박스 상태
                    if (binding.featuredCheckbox.isChecked != state.isFeatured) {
                        binding.featuredCheckbox.setOnCheckedChangeListener(null)
                        binding.featuredCheckbox.isChecked = state.isFeatured
                        binding.featuredCheckbox.setOnCheckedChangeListener(featuredCheckedChangeListener)
                    }

                    // 에러 메시지
                    state.errorMessage?.let {
                        showToast(it)
                        createViewModel.clearErrorMessage()
                    }

                    // 대표 기억 충돌 다이얼로그
                    if (state.showFeaturedConflictDialog) {
                        createViewModel.consumeFeaturedConflictDialog()
                        showFeaturedConflictDialog()
                    }

                    // 🌟 저장 성공 시 처리
                    if (state.savedSuccessfully) {
                        // 1. PresentViewModel에 데이터 전달하여 메인 리스트 갱신
                        presentViewModel.saveNewRecord(
                            photoUri = state.selectedPhotoUri ?: "",
                            memo = state.memo,
                            score = state.cesWeightedScore.toInt()
                        )

                        showToast("순간이 저장되었습니다")
                        createViewModel.resetSavedState()
                        parentFragmentManager.popBackStack() // 메인으로 복귀
                    }
                }
            }
        }
    }

    private fun showFeaturedConflictDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("이미 대표 기억이 설정되어 있습니다.\n현재 이미지를 대표 기억으로 변경하시겠습니까?")
            .setPositiveButton("변경") { _, _ -> createViewModel.confirmFeaturedReplacement(true) }
            .setNegativeButton("취소") { _, _ -> createViewModel.confirmFeaturedReplacement(false) }
            .show()
    }
}