package com.example.myapplication.feature.future

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class FutureFragment : Fragment(R.layout.fragment_future) {

    private val vm: FutureViewModel by viewModels()
    private lateinit var adapter: GoalAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 시스템 바(상태바, 내비게이션바) 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        // 리사이클러뷰 설정
        val rv = view.findViewById<RecyclerView>(R.id.recyclerGoals)
        adapter = GoalAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // 데이터 관찰
        vm.goals.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        // 추가 버튼 클릭 이벤트
        val btnAdd = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        btnAdd.setOnClickListener { showAddDialog() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddDialog() {
        // Material 테마를 입힌 Context 생성 (다이얼로그 스타일 유지)
        val contextWrapper = ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog)
        val themedInflater = LayoutInflater.from(contextWrapper)

        // 1. 차곡이가 포함된 새 XML 레이아웃 인플레이트
        val dlgView = themedInflater.inflate(R.layout.dialog_add_goal, null)

        // 2. 뷰 연결 (XML ID와 일치 확인)
        val etTitle = dlgView.findViewById<EditText>(R.id.etGoalTitle)
        val tvDate = dlgView.findViewById<TextView>(R.id.tvTargetDate)
        val btnSave = dlgView.findViewById<Button>(R.id.btnSaveGoal)
        val btnClose = dlgView.findViewById<ImageButton>(R.id.btnClose) // X 버튼

        // 날짜 초기화 (오늘 날짜)
        var selectedDate = LocalDate.now()
        tvDate.text = selectedDate.format(dateFormatter)

        // 데이트피커 리스너
        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day)
            tvDate.text = selectedDate.format(dateFormatter)
        }

        // 날짜 영역 클릭 시 피커 노출
        tvDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                datePickerListener,
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        // 다이얼로그 생성 및 설정
        val dialog = AlertDialog.Builder(contextWrapper)
            .setView(dlgView)
            .setCancelable(true)
            .create()

        // 3. 취소 로직 (X 버튼 클릭 시 닫기)
        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        // 4. 저장 로직
        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "목표를 입력하세요"
                return@setOnClickListener
            }

            vm.addGoal(title, selectedDate)
            dialog.dismiss()
        }

        // 5. 다이얼로그 노출 및 배경 투명화 처리 (Rounded Corner 적용을 위해 필수)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun updateDateText(tv: TextView, millis: Long) {
        val fmt = java.text.SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN)
        tv.text = fmt.format(Date(millis))
    }
}