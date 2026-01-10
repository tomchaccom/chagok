package com.example.myapplication.feature.future

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
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
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FutureFragment : Fragment(R.layout.fragment_future) {

    private val vm: FutureViewModel by viewModels()
    private lateinit var adapter: GoalAdapter
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter =
        DateTimeFormatter.ofPattern("yyyy.MM.dd")


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // WindowInsets: 홈버튼 영역 자동 패딩
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemBars.bottom
            )
            insets
        }


        // adapter 기반 목표 추가
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
        val ctx = requireContext()
        val dlgView = layoutInflater.inflate(R.layout.dialog_add_goal_future, null)
        val etTitle = dlgView.findViewById<EditText>(R.id.etTitle)
        val tvDate = dlgView.findViewById<TextView>(R.id.tvDate)
        val btnPick = dlgView.findViewById<Button>(R.id.btnPickDate)

        // ✅ 기본 날짜: 오늘 (LocalDate)
        var selectedDate = LocalDate.now()
        tvDate.text = selectedDate.format(dateFormatter)

        val datePickerListener =
            DatePickerDialog.OnDateSetListener { _, year, month, day ->
                // month는 0-based
                selectedDate = LocalDate.of(year, month + 1, day)
                tvDate.text = selectedDate.format(dateFormatter)
            }

        btnPick.setOnClickListener {
            DatePickerDialog(
                ctx,
                datePickerListener,
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        val dialog = AlertDialog.Builder(ctx)
            .setView(dlgView)
            .setCancelable(true)
            .create()

        dlgView.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "목표를 입력하세요"
                return@setOnClickListener
            }

            // ✅ ViewModel에 LocalDate 그대로 전달
            vm.addGoal(title, selectedDate)
            dialog.dismiss()
        }

        dlgView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateDateText(tv: TextView, millis: Long) {
        val fmt = java.text.SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN)
        tv.text = fmt.format(Date(millis))
    }
}
