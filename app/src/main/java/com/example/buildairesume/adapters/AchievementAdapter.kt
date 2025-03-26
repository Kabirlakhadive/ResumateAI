package com.example.buildairesume.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.buildairesume.databinding.ItemAchievementBinding
import com.example.buildairesume.models.Achievement

class AchievementAdapter(
    private val achievements: MutableList<Achievement>,
    private val onEdit: (Achievement, Int) -> Unit,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    inner class AchievementViewHolder(private val binding: ItemAchievementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(achievement: Achievement) {
            binding.tvTitle.text = achievement.title
            binding.tvDescription.text = achievement.description
            binding.tvDate.text = achievement.date

            binding.btnEdit.setOnClickListener {
                onEdit(achievement, adapterPosition)
            }

            binding.btnRemove.setOnClickListener {
                onRemove(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding =
            ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(achievements[position])
    }

    override fun getItemCount(): Int = achievements.size

    fun addAchievement(achievement: Achievement) {
        achievements.add(achievement)
        notifyItemInserted(achievements.size - 1)
    }

    fun updateAchievement(position: Int, achievement: Achievement) {
        achievements[position] = achievement
        notifyItemChanged(position)
    }

    fun removeAchievement(position: Int) {
        if (position in achievements.indices) {
            achievements.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setAchievements(newAchievements: List<Achievement>) {
        achievements.clear()
        achievements.addAll(newAchievements)
        notifyDataSetChanged()
    }

    fun getAchievements(): List<Achievement> = achievements
}
