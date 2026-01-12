package com.example.myapplication.feature.highlight

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemHighlightRankBinding

class HighlightRankAdapter(
    private val onItemClick: (HighlightRankItem) -> Unit
) : RecyclerView.Adapter<HighlightRankAdapter.RankViewHolder>() {

    private var items: List<HighlightRankItem> = emptyList()

    inner class RankViewHolder(private val binding: ItemHighlightRankBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HighlightRankItem) {
            binding.rankNumber.text = item.rank.toString()
            binding.rankMemo.text = if (item.memo.isNotBlank()) item.memo else "메모가 없습니다."
            binding.rankScore.text = item.score.toString()
            // TODO: HighlightRankItem에 날짜/ICP 정보가 추가되면 아래 값을 실제 데이터로 대체하세요.
            binding.rankDate.text = binding.root.context.getString(
                com.example.myapplication.R.string.highlight_rank_date_placeholder
            )
            binding.rankIcpBadge.text = binding.root.context.getString(
                com.example.myapplication.R.string.highlight_icp_badge_placeholder
            )

            if (item.photoUri.isNotBlank()) {
                binding.rankPhoto.setImageURI(Uri.parse(item.photoUri))
            } else {
                binding.rankPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankViewHolder {
        val binding = ItemHighlightRankBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RankViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<HighlightRankItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
