package com.example.myapplication.feature.highlight

import com.example.myapplication.feature.present.CreateMomentViewModel
import com.example.myapplication.feature.present.DailyRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FakeRecordRepository : RecordRepository {

    override fun getTodayRecords(): List<DailyRecord> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // TODO: Replace with Room/JSON-backed repository that filters records by date.
        return CreateMomentViewModel.getSavedRecords()
            .filter { it.date == today }
    }
}
