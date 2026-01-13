package com.example.myapplication.feature.present

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemPracticeBinding
import com.google.android.material.button.MaterialButtonToggleGroup

class PracticeAdapter(
    private val onPracticeStateChanged: (Practice, Boolean) -> Unit
) : ListAdapter<Practice, PracticeAdapter.PracticeViewHolder>(PracticeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PracticeViewHolder {
        val binding = ItemPracticeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PracticeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PracticeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PracticeViewHolder(private val binding: ItemPracticeBinding) : RecyclerView.ViewHolder(binding.root) {
        // Listener 참조를 보관하여 중복 등록/제거가 가능하도록 함
        private var toggleListener: MaterialButtonToggleGroup.OnButtonCheckedListener? = null

        fun bind(practice: Practice) {
            binding.practiceTitle.text = practice.title
            binding.practiceSubtitle.text = practice.subtitle

            // 1) 기존 리스너 제거(중복 방지)
            toggleListener?.let { binding.buttonToggleGroup.removeOnButtonCheckedListener(it) }

            // 2) 아이템 상태를 프로그램적으로 설정 (이때 리스너는 아직 등록되어 있지 않음)
            when (practice.isAchieved) {
                true -> binding.buttonToggleGroup.check(binding.achieveButton.id)
                false -> binding.buttonToggleGroup.check(binding.unachieveButton.id)
                null -> binding.buttonToggleGroup.clearChecked()
            }

            // 3) 사용자 상호작용 리스너 등록 (현재 practice 캡처)
            val listener = MaterialButtonToggleGroup.OnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    val isAchieved = checkedId == binding.achieveButton.id
                    onPracticeStateChanged(practice, isAchieved)
                }
            }
            binding.buttonToggleGroup.addOnButtonCheckedListener(listener)
            toggleListener = listener
        }
    }
}

class PracticeDiffCallback : DiffUtil.ItemCallback<Practice>() {
    override fun areItemsTheSame(oldItem: Practice, newItem: Practice): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Practice, newItem: Practice): Boolean {
        return oldItem == newItem
    }
}