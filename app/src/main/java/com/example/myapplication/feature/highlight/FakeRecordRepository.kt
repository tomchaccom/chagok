package com.example.myapplication.feature.highlight // 패키지 경로 확인

import com.example.myapplication.data.present.DailyRecord as DataRecord // 별칭 추가
import com.example.myapplication.feature.present.CreateMomentViewModel
import java.text.SimpleDateFormat
import java.util.*

class FakeRecordRepository : RecordRepository {

    // 인터페이스의 반환 타입이 DataRecord(별칭)를 사용하도록 정의되어 있어야 합니다.
    override fun getTodayRecords(): List<DataRecord> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // ViewModel에서 가져온 리스트를 DataRecord 타입으로 인식하게 합니다.
        return CreateMomentViewModel.getSavedRecords()
            .filter { it.date == today }
    }
}