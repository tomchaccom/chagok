package com.example.myapplication.data.future

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.data.past.PastRepository
import com.example.myapplication.data.past.DayEntry // 🌟 추가
import com.example.myapplication.feature.present.CreateMomentViewModel
import java.text.SimpleDateFormat
import java.util.*

class DailyCleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            val repo = PastRepository(applicationContext)
            val saved = CreateMomentViewModel.getSavedRecords()

            if (saved.isNotEmpty()) {
                val groups = saved.groupBy { it.date.ifBlank { currentDateIso() } }
                for ((date, records) in groups) {
                    val dateLabel = formatDateLabel(date)
                    // 🌟 여기 records는 List<data.present.DailyRecord>여야 함
                    val newDay = DayEntry(id = 0L, dateLabel = dateLabel, photos = records.reversed())
                    repo.addOrUpdateDayEntry(newDay)
                }

                CreateMomentViewModel.clearRecords()
                // 🌟 주의: CreateMomentViewModel에서 persistToStorage를 public으로 바꿔야 함
                CreateMomentViewModel.persistToStorage()
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun currentDateIso() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private fun formatDateLabel(dateStr: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = inFmt.parse(dateStr) ?: return dateStr
            SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault()).format(d)
        } catch (e: Exception) { dateStr }
    }
}