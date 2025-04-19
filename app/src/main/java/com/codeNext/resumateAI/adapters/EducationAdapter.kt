package com.codeNext.resumateAI.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codeNext.resumateAI.databinding.ItemEducationBinding
import com.codeNext.resumateAI.models.Education

class EducationAdapter(
    private val qualifications: MutableList<Education>,
    private val onEdit: (Education, Int) -> Unit,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<EducationAdapter.EducationViewHolder>() {

    inner class EducationViewHolder(private val binding: ItemEducationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(education: Education) {
            binding.tvQualification.text = education.qualification
            binding.tvSchool.text = education.school
            binding.tvBoard.text = education.board

            if (education.isDegreeEmpty()) {
                binding.tvDegree.visibility = View.GONE
            } else {
                binding.tvDegree.text = education.degree
            }

            if (education.isBranchEmpty()) {
                binding.tvBranch.visibility = View.GONE
            } else {
                binding.tvBranch.text = when {
                    education.isBranchEmpty() -> education.specialization
                    education.isSpecializationEmpty() -> education.branch
                    else -> "${education.branch} with ${education.specialization}"
                }
            }



            binding.tvScore.text = "${education.score} ${education.gradingSystem}"
            binding.tvYear.text = education.year

            // Handle Edit Button Click
            binding.btnEdit.setOnClickListener {
                onEdit(education, adapterPosition)
            }

            // Handle Remove Button Click
            binding.btnRemove.setOnClickListener {
                onRemove(adapterPosition)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EducationViewHolder {
        val binding =
            ItemEducationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EducationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EducationViewHolder, position: Int) {
        holder.bind(qualifications[position])
    }

    override fun getItemCount(): Int = qualifications.size

    fun addEducation(education: Education) {
        qualifications.add(education)
        notifyItemInserted(qualifications.size - 1)
    }

    fun updateEducation(position: Int, education: Education) {
        qualifications[position] = education
        notifyItemChanged(position)  // Refresh UI
    }

    fun removeEducation(position: Int) {
        if (position in qualifications.indices) {
            qualifications.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setQualifications(newQualifications: Collection<Education>) {
        qualifications.clear()
        qualifications.addAll(newQualifications)
        notifyDataSetChanged()

    }

    fun getQualifications(): List<Education> = qualifications

}