package com.example.myapplication.feature.future

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.appcompat.view.ContextThemeWrapper
import com.example.myapplication.data.future.GoalRepository

class FutureFragment : Fragment(R.layout.fragment_future) {

    private val vm: FutureViewModel by viewModels()
    private lateinit var adapter: GoalAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // GoalRepository 초기화: storage 파일이 없으면 assets 또는 더미로 생성하도록 함
        GoalRepository.initialize(requireContext())

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
        // 1. Material 테마를 입힌 Context 생성
        val contextWrapper = ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog)

        // 2. 생성한 wrapper를 사용하여 LayoutInflater 생성
        val themedInflater = LayoutInflater.from(contextWrapper)

        // 3. themedInflater를 사용하여 뷰 인플레이트
        val dlgView = themedInflater.inflate(R.layout.dialog_add_goal_future, null)

        // XML의 TextInputEditText에 맞춰 타입을 변경하거나 상위 클래스인 EditText 사용
        val etTitle = dlgView.findViewById<EditText>(R.id.etTitle)
        val tvDate = dlgView.findViewById<EditText>(R.id.tvDate)

        var selectedDate = LocalDate.now()
        tvDate.setText(selectedDate.format(dateFormatter))

        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day)
            tvDate.setText(selectedDate.format(dateFormatter))
        }
        
        // 날짜 선택 탭에서 선택된 날짜를 보여줌 -> tvDate에 저장
        tvDate.setOnClickListener {
            DatePickerDialog(
                contextWrapper,
                datePickerListener,
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        val dialog = AlertDialog.Builder(contextWrapper)
            .setView(dlgView)
            .setCancelable(true)
            .create()

        dlgView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        // XML에서 MaterialButton은 Button을 상속받으므로 그대로 유지 가능
        dlgView.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "목표를 입력하세요"
                return@setOnClickListener
            }

            vm.addGoal(title, selectedDate)
            dialog.dismiss()
        }

        dialog.show()

        // ✅ 추가: 다이얼로그 자체의 배경을 투명하게 설정하여 카드뷰의 모서리가 깎여 보이게 함
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun updateDateText(tv: TextView, millis: Long) {
        val fmt = java.text.SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN)
        tv.text = fmt.format(Date(millis))
    }
}