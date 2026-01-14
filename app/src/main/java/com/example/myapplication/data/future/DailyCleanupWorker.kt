package com.example.myapplication.data.future

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.data.past.PastRepository
import com.example.myapplication.data.past.DayEntry
// 🌟 Import Alias로 타입 충돌 방지
import com.example.myapplication.feature.present.DailyRecord as FeatureRecord
import com.example.myapplication.data.present.DailyRecord as DataRecord
import com.example.myapplication.data.present.PresentRepository
import java.text.SimpleDateFormat
import java.util.*

class DailyCleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Log.d("CleanupWorker", "🧹 과거 데이터 통합 작업 시작: ${Date()}")

        return try {
            val pastRepo = PastRepository(applicationContext)
            // 🌟 팩토리나 주입 대신 직접 생성 시 context 전달 필요 (기존 구조에 따라 조정)
            // 여기서는 앞서 만든 PresentRepository가 context를 받는다고 가정합니다.
            // 만약 PresentApi가 필요하다면 RetrofitClient.presentApi 등을 사용하세요.
            val presentRepo = PresentRepository(com.example.myapplication.core.network.RetrofitClient.presentApi, applicationContext)

            // 1. 임시 파일에서 현재 기록들을 읽어옵니다.
            // PresentRepository에 public fun getAllRecords(): List<FeatureRecord> { return _savedRecords } 가 있다고 가정
            val savedRecords = presentRepo.getTodayRecordsForWorker()

            if (savedRecords.isNotEmpty()) {
                Log.d("CleanupWorker", "데이터 발견: ${savedRecords.size}개")

                // 2. Feature 타입을 Past(Data) 타입으로 변환하며 그룹화합니다.
                val groups = savedRecords.groupBy { it.date.ifBlank { currentDateIso() } }

                for ((date, records) in groups) {
                    val dateLabel = formatDateLabel(date)

                    // FeatureRecord -> DailyRecord (Past용) 변환 로직
                    val convertedRecords = records.map { feat ->
                        com.example.myapplication.data.present.DailyRecord(
                            id = feat.id,
                            photoUri = feat.photoUri,
                            memo = feat.memo,
                            score = feat.score,
                            cesMetrics = com.example.myapplication.data.present.CesMetrics(
                                identity = feat.cesMetrics.identity,
                                connectivity = feat.cesMetrics.connectivity,
                                perspective = feat.cesMetrics.perspective,
                                weightedScore = feat.cesMetrics.weightedScore
                            ),
                            meaning = com.example.myapplication.data.present.Meaning.valueOf(feat.meaning.name),
                            date = feat.date,
                            isFeatured = feat.isFeatured
                        )
                    }

                    // 3. PastRepository에 추가 (날짜가 같으면 합쳐짐)
                    val newDay = DayEntry(id = 0L, dateLabel = dateLabel, photos = convertedRecords)
                    pastRepo.addOrUpdateDayEntry(newDay)
                }

                // 4. 과거 저장소 파일 쓰기 확정
                val isPastSaved = pastRepo.ensurePersisted()

                if (isPastSaved) {
                    // 5. 성공적으로 옮겼다면 임시 파일 및 메모리 비우기
                    presentRepo.clearAllRecords()
                    Log.d("CleanupWorker", "✅ 과거 데이터 이동 및 임시 파일 삭제 완료")
                }
            } else {
                Log.d("CleanupWorker", "처리할 기록이 없습니다.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CleanupWorker", "❌ 작업 중 에러 발생: ${e.message}")
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
        } catch (e: Exception) { dateStr }
    }
}