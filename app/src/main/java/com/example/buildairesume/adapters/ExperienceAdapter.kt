package com.example.buildairesume.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.buildairesume.databinding.ItemExerienceBinding
import com.example.buildairesume.models.Experience
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExperienceAdapter(
    private val experiences: MutableList<Experience>,
    private val onEdit: (Experience, Int) -> Unit,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ExperienceAdapter.ExperienceViewHolder>() {

    inner class ExperienceViewHolder(private val binding: ItemExerienceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(experience: Experience) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val endDate = sdf.parse(experience.endDate)
            val currentDate = Calendar.getInstance().time
            binding.tvDuration.text =
                if (experience.isEndDateEmpty() || endDate.after(currentDate)) {
                    "${experience.startDate} - till date"
                } else {
                    "${experience.startDate} - ${experience.endDate}"
                }

            binding.tvCompanyName.text = experience.companyName
            binding.tvPosition.text = experience.position
            binding.tvDescription.text = experience.description

            // Handle Edit Button Click
            binding.btnEdit.setOnClickListener {
                onEdit(experience, adapterPosition) // Pass the item and position
            }

            // Handle Remove Button Click
            binding.btnRemove.setOnClickListener {
                onRemove(adapterPosition) // Remove the item
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExperienceViewHolder {
        val binding =
            ItemExerienceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExperienceViewHolder(binding)
    }

    override fun getItemCount(): Int = experiences.size

    override fun onBindViewHolder(holder: ExperienceViewHolder, position: Int) {
        holder.bind(experiences[position])
    }

    fun addExperience(experience: Experience) {
        experiences.add(experience)
        notifyItemInserted(experiences.size - 1)
    }

    fun updateExperience(position: Int, experience: Experience) {
        experiences[position] = experience
        notifyItemChanged(position) // Refresh UI
    }

    fun removeExperience(position: Int) {
        if (position in experiences.indices) {
            experiences.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setExperiences(newExperiences: Collection<Experience>) {
        experiences.clear()
        experiences.addAll(newExperiences)
        notifyDataSetChanged() // Refresh UI
    }

    fun getExperiences(): List<Experience> = experiences

}