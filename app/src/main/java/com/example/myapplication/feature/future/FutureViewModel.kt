package com.example.myapplication.feature.future

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.future.GoalRepository
import java.time.LocalDate
// 🌟 Alias를 사용하여 data 패키지의 Goal을 명확히 구분합니다.
import com.example.myapplication.data.future.Goal as DataGoal

@RequiresApi(Build.VERSION_CODES.O)
class FutureViewModel : ViewModel() {

    // 🌟 _goals의 타입을 DataGoal(data 패키지의 Goal)로 통일해야
    // GoalRepository.getAll()의 결과값을 에러 없이 담을 수 있습니다.
    private val _goals = MutableLiveData<List<DataGoal>>(GoalRepository.getAll())
    val goals: LiveData<List<DataGoal>> = _goals

    fun addGoal(title: String, date: LocalDate) {
        // g는 DataGoal 타입입니다.
        val g = DataGoal(title = title, date = date)
        GoalRepository.add(g)

        // 갱신된 리스트를 가져와 반영합니다.
        _goals.value = GoalRepository.getAll()
    }
}