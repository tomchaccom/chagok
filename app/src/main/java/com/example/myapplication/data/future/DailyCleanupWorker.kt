package com.example.myapplication.data.future

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.feature.present.CreateMomentViewModel

class DailyCleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            // ViewModel의 companion object에 정의된 함수를 호출합니다.
            CreateMomentViewModel.performDailyCleanup()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}