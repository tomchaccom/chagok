package com.example.myapplication.feature.future

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.future.Goal
// 🌟 핵심: 반드시 'data' 패키지의 Goal을 임포트하여 타입 불일치를 해결합니다.

import com.example.myapplication.data.future.Goal as DataGoal
import com.example.myapplication.feature.future.Goal as FeatureGoal

import com.example.myapplication.data.future.GoalRepository
import com.example.myapplication.feature.present.CesMetrics
import com.example.myapplication.feature.present.DailyRecord
import com.example.myapplication.feature.present.Meaning
import com.example.myapplication.feature.present.PresentViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class FutureFragment : Fragment(R.layout.fragment_future) {

    private val vm: FutureViewModel by viewModels()
    private val presentViewModel: PresentViewModel by activityViewModels()
    private lateinit var goalAdapter: GoalAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GoalRepository.initialize(requireContext())

        val rv = view.findViewById<RecyclerView>(R.id.recyclerGoals)

        // 🌟 1. Parameter mismatch 해결: 어댑터 생성 시 콜백 전달
        goalAdapter = GoalAdapter { clickedGoal ->
            handleGoalCompletion(clickedGoal)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = goalAdapter

        // 🌟 2. Argument type mismatch 해결: 패키지 경로를 data.future.Goal로 통일
        // FutureFragment.kt 내부

        // FutureFragment.kt 내부 onViewCreated
        vm.goals.observe(viewLifecycleOwner) { list ->
            // 리스트가 비어있지 않다면 첫 번째 아이템의 타입을 확인하여 안전하게 변환
            val correctedList = list.mapNotNull { item ->
                when (item) {
                    is DataGoal -> item
                    is FeatureGoal -> {
                        // FeatureGoal을 DataGoal로 변환 (필드 복사)
                        DataGoal(
                            title = item.title,
                            date = item.date
                            // DataGoal에 isAchieved 등의 필드가 있다면 추가:
                            // isAchieved = item.isAchieved
                        )
                    }
                    else -> null
                }
            }
            goalAdapter.submitList(correctedList)
        }

        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddDialog()
        }
    }

    private fun handleGoalCompletion(goal: Goal) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val newRecord = DailyRecord(
            id = UUID.randomUUID().toString(),
            photoUri = "",
            memo = "[미래 실천] ${goal.title}",
            score = 5,
            // 🌟 3. Float type mismatch 해결: 3 -> 3.0f (또는 3f)
            cesMetrics = CesMetrics(3, 3, 3, 3.0f),
            meaning = Meaning.REMEMBER,
            date = today,
            isFeatured = false
        )

        // 🌟 4. No parameter 'uri' found 해결:
        // PresentViewModel의 saveNewRecord 정의에 맞춰 파라미터 이름을 'photoUri'로 수정합니다.
        presentViewModel.saveNewRecord(
            photoUri = newRecord.photoUri,
            memo = newRecord.memo,
            score = newRecord.score
        )

        Toast.makeText(requireContext(), "기억하기 탭으로 옮겨졌습니다!", Toast.LENGTH_SHORT).show()

        // 🌟 5. Unresolved reference 'loadGoals' 해결:
        // 필요한 경우 여기에 데이터를 다시 불러오는 vm.load() 등의 로직을 넣으세요.
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddDialog() {
        // ... (기존 showAddDialog 코드와 동일) ...
        Locale.setDefault(Locale.KOREAN)
        val contextWrapper = ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog)
        val themedInflater = LayoutInflater.from(contextWrapper)
        val dlgView = themedInflater.inflate(R.layout.dialog_add_goal, null)

        val etTitle = dlgView.findViewById<EditText>(R.id.etGoalTitle)
        val layoutDate = dlgView.findViewById<LinearLayout>(R.id.layoutDateContainer)
        val tvDate = dlgView.findViewById<TextView>(R.id.tvTargetDate)
        val btnClose = dlgView.findViewById<ImageButton>(R.id.btnClose)
        val btnSave = dlgView.findViewById<Button>(R.id.btnSaveGoal)

        var selectedDate = LocalDate.now()
        tvDate.text = selectedDate.format(dateFormatter)

        layoutDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setTheme(R.style.ThemeOverlay_App_DatePicker)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val instant = Instant.ofEpochMilli(selection)
                selectedDate = instant.atZone(ZoneId.of("UTC")).toLocalDate()
                tvDate.text = selectedDate.format(dateFormatter)
            }
            datePicker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
        }

        tvDate.setOnClickListener { layoutDate.performClick() }

        val dialog = AlertDialog.Builder(contextWrapper)
            .setView(dlgView)
            .setCancelable(true)
            .create()

        btnClose?.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "목표를 입력하세요"
                return@setOnClickListener
            }
            vm.addGoal(title, selectedDate)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}