package com.example.myapplication.feature.highlight

import com.example.myapplication.feature.present.DailyRecord

interface RecordRepository {
    fun getTodayRecords(): List<DailyRecord>
}
