package com.example.myapplication.data.future

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.data.past.PastRepository
import com.example.myapplication.data.past.DayEntry
import com.example.myapplication.feature.present.CreateMomentViewModel
import java.text.SimpleDateFormat
import java.util.*

class DailyCleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("CleanupWorker", "작업 시작됨: ${Date()}")

        return try {
            val repo = PastRepository(applicationContext)

            // ⚠️ 위험: 앱이 종료된 상태라면 ViewModel의 메모리는 비어있습니다.
            val saved = CreateMomentViewModel.getSavedRecords()

            Log.d("CleanupWorker", "가져온 기록 개수: ${saved.size}")

            if (saved.isNotEmpty()) {
                val groups = saved.groupBy { it.date.ifBlank { currentDateIso() } }

                for ((date, records) in groups) {
                    val dateLabel = formatDateLabel(date)

                    // 🌟 수정: 일단 테스트를 위해 필터링 없이 모든 기록을 넘겨보거나,
                    // 필터링 결과가 비어있는지 로그로 확인해야 합니다.
                    val achievedRecords = records.filter { it.isAchieved }

                    Log.d("CleanupWorker", "$dateLabel - 전체: ${records.size}, 완료됨: ${achievedRecords.size}")

                    if (achievedRecords.isNotEmpty()) {
                        val newDay = DayEntry(id = 0L, dateLabel = dateLabel, photos = achievedRecords)
                        repo.addOrUpdateDayEntry(newDay)
                    }
                }

                // 파일에 물리적으로 저장
                val isPersisted = repo.ensurePersisted()
                Log.d("CleanupWorker", "저장 성공 여부: $isPersisted")

                if (isPersisted) {
                    CreateMomentViewModel.clearRecords()
                    Log.d("CleanupWorker", "현재 데이터 초기화 완료")
                }
            } else {
                Log.d("CleanupWorker", "처리할 데이터가 없습니다.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CleanupWorker", "에러 발생: ${e.message}")
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun currentDateIso() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun formatDateLabel(dateStr: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = inFmt.parse(dateStr) ?: return dateStr
            SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault()).format(d)
        } catch (e: Exception) {
            dateStr
        }
    }
}