package com.example.myapplication.feature.highlight


// 여기에도 별칭 임포트를 추가해야 합니다.
import com.example.myapplication.data.present.DailyRecord as DataRecord

interface RecordRepository {
    // 반환 타입을 DataRecord(별칭)로 변경합니다.
    fun getTodayRecords(): List<DataRecord>
}