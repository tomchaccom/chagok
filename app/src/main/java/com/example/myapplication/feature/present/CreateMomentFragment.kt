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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
        private const val ARG_GOAL_TITLE = "GOAL_TITLE" // 🌟 추가된 인자

        @JvmStatic
        fun newInstance(recordId: String) = CreateMomentFragment().apply {
            arguments = Bundle().apply { putString(ARG_RECORD_ID, recordId) }
        }
    }

    private val createViewModel: CreateMomentViewModel by viewModels()
    private val presentViewModel: PresentViewModel by activityViewModels()

    private var currentPhotoFile: File? = null
    private var featuredCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    /* ---------------- 권한 및 런처 설정 (기존과 동일) ---------------- */
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera() else Toast.makeText(requireContext(), "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handleSelectedPhoto(uri) }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && currentPhotoFile?.exists() == true) {
            val photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", currentPhotoFile!!)
            handleSelectedPhoto(photoUri)
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCreateMomentBinding {
        return FragmentCreateMomentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🌟 [추가] 실천 버튼을 통해 넘어온 경우 제목 세팅
        val goalTitle = arguments?.getString(ARG_GOAL_TITLE) ?: ""
        if (goalTitle.isNotEmpty()) {
            val initialMemo = "[실천] $goalTitle"
            binding.memoEditText.setText(initialMemo)
            createViewModel.setMemo(initialMemo) // ViewModel 상태와 동기화
        }

        setupToolbar()
        initSliderTexts()
        setupCesSliders()
        setupPhotoButtons()
        setupMemoInput()
        setupFeaturedCheckbox()
        setupSaveButton()
        observeViewModel()
        observeUiState()
    }

    /* ---------------- UI 및 이벤트 설정 (기존 기능 복구) ---------------- */

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
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
        // 1. Identity (정체성) 슬라이더
        binding.layoutIdentity.sliderMain.addOnChangeListener { _, value, _ ->
            val msg1 = when(value.toInt()) {
                1, 2 -> "조금은 낯선 모습이었나요?"
                3 -> "평소의 당신다운 모습이네요."
                else -> "완벽하게 '나'다운 순간이었어요!"
            }
            updateChagok(msg1, value) // 상단 말풍선 메시지 업데이트
            createViewModel.setCesIdentity(value.toInt()) // ViewModel 상태 업데이트 및 점수 자동 계산
        }

        // 2. Connectivity (연결성) 슬라이더
        binding.layoutConnectivity.sliderMain.addOnChangeListener { _, value, _ ->
            val msg2 = when(value.toInt()) {
                1, 2 -> "주변과 조금 떨어져 있는 기분이었나요?"
                3 -> "적당한 거리에서 함께 호흡했네요."
                else -> "모든 순간이 긴밀하게 연결된 느낌이었어요!"
            }
            updateChagok(msg2, value)
            createViewModel.setCesConnectivity(value.toInt())
        }

        // 3. Perspective (관점) 슬라이더
        binding.layoutPerspective.sliderMain.addOnChangeListener { _, value, _ ->
            val msg3 = when(value.toInt()) {
                1, 2 -> "하나의 모습에 집중한 시간이었나요?"
                3 -> "균형 잡힌 시선으로 바라보았네요."
                else -> "더 넓은 세상을 발견한 값진 순간이었어요!"
            }
            updateChagok(msg3, value)
            createViewModel.setCesPerspective(value.toInt())
        }
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            createViewModel.uiState.collect { state ->
                // 종합 점수 텍스트 갱신 (예: 3.5점)
                binding.cesScoreValue.text = String.format("%.1f점", state.cesWeightedScore)

                // 점수 설명 갱신 (예: 보통)
                binding.cesScoreDescription.text = "(${state.cesDescription})"

                // 저장 성공 시 화면 종료 등 추가 로직
                if (state.savedSuccessfully) {
                    parentFragmentManager.popBackStack()
                }
            }
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
                Toast.makeText(requireContext(), "사진을 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createViewModel.saveMoment()
        }
    }

    /* ---------------- 사진 처리 로직 (기존과 동일) ---------------- */
    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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

    /* ---------------- UI 상태 관찰 및 저장 성공 처리 ---------------- */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                createViewModel.uiState.collect { state ->
                    // 1. 사진 미리보기 업데이트
                    if (!state.selectedPhotoUri.isNullOrBlank()) {
                        binding.photoPreview.setImageURI(state.selectedPhotoUri.toUri())
                        binding.photoPlaceholder.visibility = View.GONE
                    }

                    // 2. 🌟 저장 성공 시 처리 (중복 제거 및 로직 통합)
                    if (state.savedSuccessfully) {
                        // 전달받은 인자 꺼내기
                        val goalId = arguments?.getString("GOAL_ID")

                        // PresentViewModel의 saveNewRecord 하나로 모든 처리를 위임합니다.
                        // (기록 저장 + 오늘 중복 체크 + 미래 목표 완료 처리)
                        presentViewModel.saveNewRecord(
                            photoUri = state.selectedPhotoUri ?: "",
                            memo = state.memo,
                            score = state.cesWeightedScore.toInt(),
                            goalId = goalId
                        )

                        Toast.makeText(requireContext(), "순간이 기록되었습니다!", Toast.LENGTH_SHORT).show()

                        // ViewModel 상태 초기화 (연속 호출 방지)
                        createViewModel.resetSavedState()

                        // 현재 화면 종료 (기록 탭 메인으로 복귀)
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }
    }
}