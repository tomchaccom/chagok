package com.example.myapplication.feature.future

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class FutureFragment : Fragment(R.layout.fragment_future) {

    private val vm: FutureViewModel by viewModels()
    private lateinit var adapter: GoalAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun showAddDialog() {
        val ctx = requireContext()
        val dlgView = layoutInflater.inflate(R.layout.dialog_add_goal, null)
        val etTitle = dlgView.findViewById<EditText>(R.id.etTitle)
        val tvDate = dlgView.findViewById<TextView>(R.id.tvDate)
        val btnPick = dlgView.findViewById<Button>(R.id.btnPickDate)

        // 기본 날짜: 오늘
        val cal = Calendar.getInstance()
        updateDateText(tvDate, cal.timeInMillis)

        val datePickerListener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            cal.set(y, m, d)
            updateDateText(tvDate, cal.timeInMillis)
        }

        btnPick.setOnClickListener {
            DatePickerDialog(requireContext(),
                datePickerListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
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
            vm.addGoal(title, cal.timeInMillis)
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
