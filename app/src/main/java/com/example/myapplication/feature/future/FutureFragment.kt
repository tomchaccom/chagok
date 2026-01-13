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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        val rv = view.findViewById<RecyclerView>(R.id.recyclerGoals)
        adapter = GoalAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        vm.goals.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        val btnAdd = view.findViewById<FloatingActionButton>(R.id.fabAdd)
        btnAdd.setOnClickListener { showAddDialog() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddDialog() {

        // showAddDialog() 함수 시작 부분에 추가
        Locale.setDefault(Locale.KOREAN)
        val contextWrapper = ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog)
        val themedInflater = LayoutInflater.from(contextWrapper)
        val dlgView = themedInflater.inflate(R.layout.dialog_add_goal, null)

        val etTitle = dlgView.findViewById<EditText>(R.id.etGoalTitle)
        // 변수명을 layoutDate로 가져왔습니다.
        val layoutDate = dlgView.findViewById<LinearLayout>(R.id.layoutDateContainer)
        val tvDate = dlgView.findViewById<TextView>(R.id.tvTargetDate)
        val btnClose = dlgView.findViewById<ImageButton>(R.id.btnClose)
        val btnSave = dlgView.findViewById<Button>(R.id.btnSaveGoal)

        var selectedDate = LocalDate.now()
        tvDate.text = selectedDate.format(dateFormatter)

        // 1. layoutDate (변수명 일치) 하나에만 클릭 리스너를 설정
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

            if (!datePicker.isAdded) {
                datePicker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
            }
        }

        // 2. 텍스트를 눌러도 레이아웃 클릭이 실행되도록 연결
        tvDate.setOnClickListener {
            layoutDate.performClick()
        }

        val dialog = AlertDialog.Builder(contextWrapper)
            .setView(dlgView)
            .setCancelable(true)
            .create()

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

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