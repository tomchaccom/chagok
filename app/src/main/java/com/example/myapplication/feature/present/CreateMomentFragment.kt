package com.example.myapplication.feature.present

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.example.myapplication.databinding.FragmentCreateMomentBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * CreateMomentFragment
 *
 * 사용자가 "순간 기록하기"를 통해 아래를 입력합니다:
 * 1. 사진 (필수) - 갤러리 또는 카메라
 * 2. 메모 (선택) - EditText
 * 3. 점수 (필수) - Slider (1~10)
 * 4. 기억/잊기 선택 (필수) - 버튼
 * 5. 오늘의 대표 기억 (선택) - CheckBox
 *
 * 저장하면 PresentFragment로 돌아감
 */
class CreateMomentFragment : BaseFragment<FragmentCreateMomentBinding>() {

    private val viewModel: CreateMomentViewModel by viewModels()
    private var capturedImageUri: Uri? = null
    private var currentPhotoFile: File? = null

    /**
     * 권한 요청 처리
     * Android 6.0 (API 23) 이상에서 CAMERA는 Dangerous Permission이므로 런타임 요청 필요
     */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한 승인됨 → 카메라 실행
            launchCamera()
        } else {
            // 권한 거절됨
            showToast("카메라 권한이 필요합니다. 설정에서 허용해주세요.")
        }
    }

    // ...existing code...
    // 순수 갤러리 앱만 열기 (ACTION_PICK 사용)
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data
                val selectedImageUri = data?.data
                if (selectedImageUri != null) {
                    viewModel.setSelectedPhoto(selectedImageUri.toString())
                    showToast("사진이 선택되었습니다")
                }
            }
        } catch (e: Exception) {
            showToast("갤러리 접근 중 오류: ${e.message}")
        }
    }

    // 카메라 촬영 결과 처리
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                if (currentPhotoFile != null && currentPhotoFile!!.exists()) {
                    val photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        currentPhotoFile!!
                    )
                    viewModel.setSelectedPhoto(photoUri.toString())
                    showToast("사진 촬영 완료")
                } else {
                    showToast("사진 저장 중 오류가 발생했습니다")
                }
            } else {
                showToast("사진 촬영이 취소되었습니다")
            }
        } catch (e: Exception) {
            showToast("카메라 처리 중 오류: ${e.message}")
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCreateMomentBinding {
        return FragmentCreateMomentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupSlider()
        setupMeaningButtons()
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

    /**
     * 점수 슬라이더 설정
     * - 1 ~ 10 범위
     * - 현재값을 scoreValue TextView에 표시
     */
    private fun setupSlider() {
        binding.scoreSlider.valueFrom = 1f
        binding.scoreSlider.valueTo = 10f
        binding.scoreSlider.addOnChangeListener { _, value, _ ->
            val score = value.toInt()
            binding.scoreValue.text = score.toString()
            viewModel.setScore(score)
        }
        // 초기값 설정
        binding.scoreSlider.value = viewModel.uiState.value.score.toFloat()
        binding.scoreValue.text = viewModel.uiState.value.score.toString()
    }

    /**
     * 기억/잊기 버튼 설정
     * - rememberButton: 기억하기
     * - forgetButton: 잊기
     *
     * 클릭 시 선택 상태를 UI로 표시 (배경색 변경 등)
     */
    private fun setupMeaningButtons() {
        binding.rememberButton.setOnClickListener {
            viewModel.setMeaning(Meaning.REMEMBER)
            updateMeaningButtonsUI()
        }

        binding.forgetButton.setOnClickListener {
            viewModel.setMeaning(Meaning.FORGET)
            updateMeaningButtonsUI()
        }

        // 초기 상태 UI 업데이트
        updateMeaningButtonsUI()
    }

    /**
     * 기억/잊기 버튼의 시각적 상태 업데이트
     * - 선택된 버튼: 강조 (배경색, 텍스트 색상)
     * - 미선택 버튼: 일반
     */
    private fun updateMeaningButtonsUI() {
        val currentMeaning = viewModel.uiState.value.meaning
        if (currentMeaning == Meaning.REMEMBER) {
            binding.rememberButton.setBackgroundColor(requireContext().getColor(R.color.primary))
            binding.rememberButton.setTextColor(requireContext().getColor(android.R.color.white))
            binding.forgetButton.setBackgroundColor(requireContext().getColor(android.R.color.white))
            binding.forgetButton.setTextColor(requireContext().getColor(R.color.text_primary))
        } else {
            binding.forgetButton.setBackgroundColor(requireContext().getColor(R.color.error))
            binding.forgetButton.setTextColor(requireContext().getColor(android.R.color.white))
            binding.rememberButton.setBackgroundColor(requireContext().getColor(android.R.color.white))
            binding.rememberButton.setTextColor(requireContext().getColor(R.color.text_primary))
        }
    }

    /**
     * 사진 선택 버튼 설정
     * - changePhotoButton(갤러리): 기본 갤러리 앱 (ACTION_PICK)
     * - cameraButton(카메라): 기본 카메라 앱 (ACTION_IMAGE_CAPTURE) + 권한 확인
     */
    private fun setupPhotoButtons() {
        // 갤러리 버튼: Intent.ACTION_PICK으로 기본 갤러리만 열기
        binding.changePhotoButton.setOnClickListener {
            try {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryIntent.type = "image/*"
                galleryLauncher.launch(galleryIntent)
            } catch (e: Exception) {
                showToast("갤러리를 열 수 없습니다: ${e.message}")
            }
        }

        // 카메라 버튼: 권한 확인 → 카메라 실행
        binding.cameraButton.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
    }

    /**
     * 카메라 권한 확인 및 요청
     *
     * 흐름:
     * 1. 이미 권한이 있으면 → 즉시 카메라 실행
     * 2. 권한이 없으면 → 사용자에게 요청
     * 3. 사용자 응답 → cameraPermissionLauncher 콜백 처리
     */
    private fun checkCameraPermissionAndLaunch() {
        val permission = Manifest.permission.CAMERA

        when {
            // ✅ 이미 권한이 있음 → 즉시 카메라 실행
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }

            // ❌ 권한이 없음 → 사용자에게 요청
            else -> {
                cameraPermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * 카메라 Intent 실행 (권한 확인 후 호출)
     */
    private fun launchCamera() {
        try {
            // 1. 카메라 앱이 존재하는지 확인
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(requireActivity().packageManager) == null) {
                showToast("이 기기에 카메라 앱이 없습니다")
                return
            }

            // 2. 촬영된 사진을 저장할 파일 생성
            try {
                currentPhotoFile = createImageFile()
            } catch (e: Exception) {
                showToast("사진 저장 디렉토리를 만들 수 없습니다: ${e.message}")
                return
            }

            // 3. FileProvider를 사용하여 Uri 생성
            val photoUri = try {
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    currentPhotoFile!!
                )
            } catch (e: Exception) {
                showToast("파일 접근 권한 오류: ${e.message}")
                return
            }

            // 4. 카메라에 저장 경로 지정
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            // 5. 카메라 실행
            cameraLauncher.launch(cameraIntent)

        } catch (e: Exception) {
            showToast("카메라를 실행할 수 없습니다: ${e.message}")
        }
    }


    /**
     * 대표 기억 체크박스 설정
     */
    private fun setupFeaturedCheckbox() {
        binding.featuredCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.toggleFeatured()
            } else {
                // 이미 toggleFeatured에서 상태 반전이 되므로
                // 체크 상태와 ViewModel 상태를 동기화
                if (viewModel.uiState.value.isFeatured) {
                    viewModel.toggleFeatured()
                }
            }
        }
    }

    /**
     * 메모 입력 필드 설정
     */
    private fun setupMemoInput() {
        binding.memoEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setMemo(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    /**
     * 카메라 촬영 결과를 저장할 임시 이미지 파일 생성
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        // 앱의 외부 캐시 디렉토리 사용 (권한 필요 없음)
        val storageDir = requireContext().getExternalCacheDir()

        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /**
     * 저장 버튼 설정
     * - 유효성 검사 후 viewModel.saveMoment() 호출
     * - 저장 완료 시 PresentFragment로 복귀
     */
    private fun setupSaveButton() {
        binding.saveMomentButton.setOnClickListener {
            val state = viewModel.uiState.value

            // 사진 필수 검사
            if (state.selectedPhotoUri.isNullOrBlank()) {
                showToast("사진을 선택해주세요")
                return@setOnClickListener
            }

            // 저장 실행
            viewModel.saveMoment()
        }
    }

    /**
     * ViewModel의 uiState 관찰
     * - 선택된 사진 미리보기 표시
     * - 에러 메시지 표시
     * - 버튼 활성화/비활성화 관리
     * - 저장 완료 시 PresentFragment로 복귀
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 사진 미리보기
                    if (!state.selectedPhotoUri.isNullOrBlank()) {
                        val uri = state.selectedPhotoUri.toUri()
                        binding.photoPreview.setImageURI(uri)
                        binding.photoPlaceholder.isVisible = false
                    } else {
                        binding.photoPlaceholder.isVisible = true
                    }

                    // 버튼 활성화 상태 관리
                    // 저장 중이 아닐 때만 버튼 활성화
                    val isButtonsEnabled = !state.isLoading
                    binding.changePhotoButton.isEnabled = isButtonsEnabled
                    binding.cameraButton.isEnabled = isButtonsEnabled
                    binding.saveMomentButton.isEnabled = isButtonsEnabled

                    // 에러 메시지
                    if (state.errorMessage != null) {
                        showToast(state.errorMessage)
                        viewModel.clearErrorMessage()
                    }

                    // 저장 완료 → PresentFragment로 복귀
                    if (state.savedSuccessfully) {
                        showToast("순간이 저장되었습니다!")
                        viewModel.resetSavedState()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }
}