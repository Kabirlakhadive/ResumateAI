package com.example.buildairesume.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.buildairesume.databinding.ItemProjectBinding
import com.example.buildairesume.models.Project

class ProjectAdapter(
    private val projects: MutableList<Project>,
    private val onEdit: (Project, Int) -> Unit,
    private val onRemove: (Int) -> Unit
) :
    RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.tvTitle.text = project.title
            binding.tvDescription.text = project.description
            binding.tvTechStack.text = project.techStack

            // handle edit button
            binding.btnEdit.setOnClickListener {
                onEdit(project, adapterPosition)
            }

            // handle remove button
            binding.btnRemove.setOnClickListener {
                onRemove(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projects[position])
    }

    override fun getItemCount(): Int = projects.size

    fun addProject(project: Project) {
        projects.add(project)
        notifyItemInserted(projects.size - 1)
    }

    fun updateProject(position: Int, project: Project) {
        projects[position] = project
        notifyItemChanged(position)  // Refresh UI
    }

    fun removeProject(position: Int) {
        if (position in projects.indices) {
            projects.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setProjects(newProjects: Collection<Project>) {
        projects.clear()
        projects.addAll(newProjects)
        notifyDataSetChanged() // Refresh UI

    }

    fun getProjects(): List<Project> = projects

}
