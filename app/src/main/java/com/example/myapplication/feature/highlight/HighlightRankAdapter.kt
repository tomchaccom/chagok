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

            val normalizedScore = (item.score.coerceIn(1, 10) - 1) / 9f
            binding.rankEmphasis.alpha = 0.4f + normalizedScore * 0.6f
            binding.rankEmphasis.scaleY = 0.7f + normalizedScore * 0.3f

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
