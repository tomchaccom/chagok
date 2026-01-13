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

        // 1. 슬라이더 레이아웃별 텍스트 초기화
        initSliderTexts()

        // 2. 리스너 및 UI 설정 함수 호출 (이게 빠지면 에러가 납니다)
        setupToolbar()
        setupCesSliders()     // 슬라이더 리스너 연결
        setupPhotoButtons()   // 카메라/갤러리 버튼 리스너 연결
        setupMemoInput()      // 메모 입력 리스너 연결
        setupFeaturedCheckbox()
        setupSaveButton()

        // 3. ViewModel 상태 관찰 시작
        observeUiState()
    }

    private fun initSliderTexts() {
        // Identity 초기화
        binding.layoutIdentity.sliderTitle.text = "Identity (정체성)"
        binding.layoutIdentity.sliderSubTitle.text = "얼마나 '나'다운 기억이었나요?"

        // Connectivity 초기화
        binding.layoutConnectivity.sliderTitle.text = "Connectivity (연결성)"
        binding.layoutConnectivity.sliderSubTitle.text = "무의식적으로 떠오를 것 같은 기억인가요?"

        // Perspective 초기화
        binding.layoutPerspective.sliderTitle.text = "Perspective (관점)"
        binding.layoutPerspective.sliderSubTitle.text = "이 기억이 앞날의 당신을 변화시킬 수 있나요?"
    }

    /**
     * CES(Identity, Connectivity, Perspective) 슬라이더 초기화 및 에러 수정
     */
    private fun setupCesSliders() {
        // 1. Identity (나다움)
        binding.layoutIdentity.sliderMain.addOnChangeListener { _, value, _ ->
            val msg = when(value.toInt()) {
                1, 2 -> "조금은 낯선 모습이었나요?"
                3 -> "평소의 당신다운 모습이네요."
                else -> "완벽하게 '나'다운 순간이었어요!"
            }
            updateChagok(msg, value)
            binding.layoutIdentity.sliderValueText.text = value.toInt().toString()
            viewModel.setCesIdentity(value.toInt())
        }

        // 2. Connectivity (연결성)
        binding.layoutConnectivity.sliderMain.addOnChangeListener { _, value, _ ->
            val msg = when(value.toInt()) {
                1, 2 -> "혼자만의 깊은 시간이었군요."
                3 -> "세상과 기분 좋게 연결된 느낌!"
                else -> "모든 것이 하나로 이어진 듯해요."
            }
            updateChagok(msg, value)
            binding.layoutConnectivity.sliderValueText.text = value.toInt().toString()
            viewModel.setCesConnectivity(value.toInt())
        }

        // 3. Perspective (관점의 확장)
        binding.layoutPerspective.sliderMain.addOnChangeListener { _, value, _ ->
            val msg = when(value.toInt()) {
                1, 2 -> "익숙하고 편안한 시선이었어요."
                3 -> "새로운 생각을 해보게 되었네요."
                else -> "세상을 보는 눈이 한 뼘 더 커졌어요!"
            }
            updateChagok(msg, value)
            binding.layoutPerspective.sliderValueText.text = value.toInt().toString()
            viewModel.setCesPerspective(value.toInt())
        }
    }

    // 공통 애니메이션 및 메시지 업데이트 함수
    private fun updateChagok(message: String, value: Float) {
        binding.tvChagokMessage.text = message

        // 반응형 애니메이션: 점수가 높을수록 더 크게 반응!
        val scaleFactor = 1.1f + (value * 0.04f)
        binding.ivChagokEmo.animate()
            .scaleX(scaleFactor)
            .scaleY(scaleFactor)
            .setDuration(150)
            .withEndAction {
                binding.ivChagokEmo.animate().scaleX(1.0f).scaleY(1.0f).start()
            }.start()
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

                    // 1. 사진 미리보기 및 플레이스홀더 (ID: photo_placeholder -> photoPlaceholder)
                    if (!state.selectedPhotoUri.isNullOrBlank()) {
                        binding.photoPreview.setImageURI(state.selectedPhotoUri.toUri())
                        // isVisible 속성이 안 보인다면 view.visibility = View.GONE 사용
                        binding.photoPlaceholder.visibility = android.view.View.GONE
                    } else {
                        binding.photoPlaceholder.visibility = android.view.View.VISIBLE
                    }

                    // 2. CES 값 업데이트 (레이아웃 내부 참조)
                    binding.layoutIdentity.sliderValueText.text = state.cesInput.identity.toString()
                    binding.layoutConnectivity.sliderValueText.text = state.cesInput.connectivity.toString()
                    binding.layoutPerspective.sliderValueText.text = state.cesInput.perspective.toString()

                    // 3. 메모 입력창 업데이트
                    if (binding.memoEditText.text?.toString() != state.memo) {
                        binding.memoEditText.setText(state.memo)
                        binding.memoEditText.setSelection(state.memo.length)
                    }

                    // 4. 종합 점수 및 설명 업데이트 (s 개수 오타 주의!)
                    // XML ID가 ces_score_value이므로 바인딩은 cesScoreValue입니다.
                    binding.cesScoreValue.text = "${state.cesWeightedScore}점"
                    binding.cesScoreDescription.text = state.cesDescription

                    // 5. 슬라이더 바 값 동기화 (레이아웃 내부 참조)
                    if (binding.layoutIdentity.sliderMain.value != state.cesInput.identity.toFloat()) {
                        binding.layoutIdentity.sliderMain.value = state.cesInput.identity.toFloat()
                    }
                    if (binding.layoutConnectivity.sliderMain.value != state.cesInput.connectivity.toFloat()) {
                        binding.layoutConnectivity.sliderMain.value = state.cesInput.connectivity.toFloat()
                    }
                    if (binding.layoutPerspective.sliderMain.value != state.cesInput.perspective.toFloat()) {
                        binding.layoutPerspective.sliderMain.value = state.cesInput.perspective.toFloat()
                    }

                    // 6. 버튼 및 체크박스 상태
                    val enabled = !state.isLoading
                    binding.saveMomentButton.isEnabled = enabled

                    if (binding.featuredCheckbox.isChecked != state.isFeatured) {
                        binding.featuredCheckbox.setOnCheckedChangeListener(null)
                        binding.featuredCheckbox.isChecked = state.isFeatured
                        binding.featuredCheckbox.setOnCheckedChangeListener(featuredCheckedChangeListener)
                    }

                    // 7. 기타 처리 (에러, 다이얼로그, 저장 완료)
                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearErrorMessage()
                    }

                    if (state.showFeaturedConflictDialog) {
                        viewModel.consumeFeaturedConflictDialog()
                        showFeaturedConflictDialog()
                    }

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

    private fun setupToolbar() {
        // 툴바의 네비게이션 아이콘(보통 X 또는 뒤로가기 화살표) 클릭 리스너
        binding.toolbar.setNavigationOnClickListener {
            // 현재 프래그먼트를 제거하고 이전 화면으로 복귀
            parentFragmentManager.popBackStack()
        }
    }
}
