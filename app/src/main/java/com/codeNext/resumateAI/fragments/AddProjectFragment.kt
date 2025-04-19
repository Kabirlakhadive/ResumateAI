package com.codeNext.resumateAI.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.codeNext.resumateAI.databinding.FragmentAddProjectBinding
import com.codeNext.resumateAI.models.Project

class AddProjectFragment(
    private val onAdd: (Project) -> Unit,
    private val onEdit: ((Project, Int) -> Unit)? = null,
    private val projectToEdit: Project? = null,
    private val position: Int? = null
) : DialogFragment() {

    private var _binding: FragmentAddProjectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill data if editing
        projectToEdit?.let {
            binding.etTitle.setText(it.title)
            binding.etDescription.setText(it.description)
            binding.etTechStack.setText(it.techStack)
            binding.btnAdd.text = "Update"
        }

        binding.btnAdd.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val techStack = binding.etTechStack.text.toString()

            if (title.isNotEmpty() && description.isNotEmpty() && techStack.isNotEmpty()) {
                val newProject = Project(
                    projectId = projectToEdit?.projectId, // Keep ID if editing
                    title = title,
                    description = description,
                    techStack = techStack,
                    output = null
                )

                if (projectToEdit != null && position != null) {
                    onEdit?.invoke(newProject, position)
                } else {
                    onAdd(newProject)
                }
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
