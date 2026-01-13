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

        android.util.Log.d("CleanupWorker", "작업 시작됨: ${java.util.Date()}")
        return try {
            val repo = PastRepository(applicationContext)
            val saved = CreateMomentViewModel.getSavedRecords()


            if (saved.isNotEmpty()) {
                val groups = saved.groupBy { it.date.ifBlank { currentDateIso() } }
                for ((date, records) in groups) {
                    val dateLabel = formatDateLabel(date)
                    // 🌟 실천 완료(isAchieved)된 기록만 과거로 넘기는 필터링 추가 권장
                    val achievedRecords = records.filter { it.isAchieved }
                    if (achievedRecords.isNotEmpty()) {
                        val newDay = DayEntry(id = 0L, dateLabel = dateLabel, photos = achievedRecords)
                        repo.addOrUpdateDayEntry(newDay)
                    }
                }

                // 🌟 중요: 먼저 PastRepository의 변경사항을 확실히 파일에 기록
                repo.ensurePersisted()

                // 그 다음 현재 데이터를 비움
                CreateMomentViewModel.clearRecords()
            }
            android.util.Log.d("CleanupWorker", "과거 데이터 통합 성공")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("CleanupWorker", "에러 발생: ${e.message}")
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