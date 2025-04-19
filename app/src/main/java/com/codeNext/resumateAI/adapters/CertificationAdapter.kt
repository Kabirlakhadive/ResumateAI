package com.codeNext.resumateAI.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codeNext.resumateAI.databinding.ItemCertificationBinding
import com.codeNext.resumateAI.models.Certification

class CertificationAdapter(
    private val certifications: MutableList<Certification>,
    private val onEdit: (Certification, Int) -> Unit,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<CertificationAdapter.CertificationViewHolder>() {

    inner class CertificationViewHolder(private val binding: ItemCertificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(certification: Certification) {
            binding.tvName.text = certification.name
            binding.tvYear.text = certification.year


            // Handle Edit Button Click
            binding.btnEdit.setOnClickListener {
                onEdit(certification, adapterPosition) // Pass the item and position
            }

            // Handle Remove Button Click
            binding.btnRemove.setOnClickListener {
                onRemove(adapterPosition) // Remove the item
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CertificationViewHolder {
        val binding =
            ItemCertificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CertificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CertificationViewHolder, position: Int) {
        holder.bind(certifications[position])
    }

    override fun getItemCount(): Int = certifications.size

    fun addCertification(certification: Certification) {
        certifications.add(certification)
        notifyItemInserted(certifications.size - 1)
    }

    fun updateCertification(position: Int, certification: Certification) {
        certifications[position] = certification
        notifyItemChanged(position) // Refresh UI
    }

    fun removeCertification(position: Int) {
        if (position in certifications.indices) {
            certifications.removeAt(position)
            notifyItemRemoved(position)
        }
    }


    fun setCertifications(newCertifications: Collection<Certification>) {
        certifications.clear()
        certifications.addAll(newCertifications)
        notifyDataSetChanged() // Refresh UI
    }

    fun getCertifications(): List<Certification> = certifications

}
