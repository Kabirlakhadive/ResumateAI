package com.codeNext.resumateAI.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.codeNext.resumateAI.R
import com.codeNext.resumateAI.models.TemplateItem

class TemplateAdapter(
    private val templates: List<TemplateItem>,
    private val onClick: (TemplateItem, View) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class TemplateViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.iv_template)
        private val cardView: CardView = view.findViewById(R.id.cv_template)

        fun bind(template: TemplateItem, position: Int) {
            imageView.setImageResource(template.imageResId)

            // Change background color if selected
            if (position == selectedPosition) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(view.context, R.color.blue))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(view.context, R.color.gray))
            }

            view.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position

                notifyItemChanged(previousSelected) // Reset old selection
                notifyItemChanged(selectedPosition) // Highlight new selection

                onClick(template, cardView)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_template, parent, false)
        return TemplateViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(templates[position], position)
    }

    override fun getItemCount() = templates.size
}
