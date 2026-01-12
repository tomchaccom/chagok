package com.example.myapplication.feature.past

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.past.PhotoItem

class PhotoAdapter(
    private val onPhotoClick: (position: Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoVH>() {

    private var items: List<PhotoItem> = emptyList()
    private var selectedIndex: Int? = null

    fun submitList(list: List<PhotoItem>) {
        items = list
        selectedIndex = null
        notifyDataSetChanged()
    }

    fun setSelectedIndex(index: Int?) {
        selectedIndex = index
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_grid, parent, false)
        return PhotoVH(v)
    }

    override fun onBindViewHolder(holder: PhotoVH, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class PhotoVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val overlay: View = itemView.findViewById(R.id.overlay)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onPhotoClick(pos)
                }
            }
        }

        fun bind(photo: PhotoItem, position: Int) {
            val ctx = ivPhoto.context
            val uriStr = photo.imageUri
            try {
                if (uriStr.startsWith("android.resource://")) {
                    val lastSeg = uriStr.substringAfterLast('/')
                    val nameNoExt = lastSeg.substringBeforeLast('.', lastSeg)
                    val sanitized = nameNoExt.replace(Regex("[^a-z0-9_]+"), "_").lowercase()
                    val resId = ctx.resources.getIdentifier(sanitized, "drawable", ctx.packageName)
                    if (resId != 0) {
                        ivPhoto.setImageResource(resId)
                        ivPhoto.visibility = View.VISIBLE
                    } else {
                        ivPhoto.setImageURI(uriStr.toUri())
                        ivPhoto.visibility = View.VISIBLE
                    }
                } else {
                    ivPhoto.setImageURI(uriStr.toUri())
                    ivPhoto.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                ivPhoto.setImageResource(android.R.drawable.ic_menu_report_image)
                ivPhoto.visibility = View.VISIBLE
            }

            val isSelected = (selectedIndex == position)
            overlay.isVisible = isSelected
            overlay.setBackgroundColor(if (isSelected) Color.parseColor("#330000FF") else Color.TRANSPARENT)
        }
    }
}
