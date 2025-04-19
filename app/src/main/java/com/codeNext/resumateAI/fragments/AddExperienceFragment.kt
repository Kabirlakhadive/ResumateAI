package com.codeNext.resumateAI.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.codeNext.resumateAI.databinding.FragmentAddExperienceBinding
import com.codeNext.resumateAI.models.Experience
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExperienceFragment(
    private val onAdd: (Experience) -> Unit,
    private val onEdit: ((Experience, Int) -> Unit)? = null,
    private val experienceToEdit: Experience? = null,
    private val position: Int? = null
) : DialogFragment() {

    private var _binding: FragmentAddExperienceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExperienceBinding.inflate(inflater, container, false)
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
        experienceToEdit?.let {
            binding.etCompanyName.setText(it.companyName)
            binding.etStartDate.setText(it.startDate)
            binding.etEndDate.setText(it.endDate)
            binding.etPosition.setText(it.position)
            binding.etDescription.setText(it.description)
            binding.btnAdd.text = "Update"
        }

        binding.btnAdd.setOnClickListener {
            val companyName = binding.etCompanyName.text.toString()
            val startDate = binding.etStartDate.text.toString()
            val endDate = binding.etEndDate.text.toString()
            val post = binding.etPosition.text.toString()
            val description = binding.etDescription.text.toString()

            if (companyName.isNotEmpty() && startDate.isNotEmpty() && post.isNotEmpty()) {
                val newExperience = Experience(
                    experienceId = experienceToEdit?.experienceId, // Keep ID if editing
                    companyName = companyName,
                    startDate = startDate,
                    endDate = endDate,
                    position = post,
                    description = description,
                    output = null
                )

                if (experienceToEdit != null && position != null) {
                    onEdit?.invoke(newExperience, position)
                } else {
                    onAdd(newExperience)
                }
                dismiss()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please fill all required fields",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnCancel.setOnClickListener { dismiss() }
        setupDatePickers()
    }

    private fun setupDatePickers() {
        setupDatePicker(binding.etStartDate)
        setupDatePicker(binding.etEndDate)
    }

    private fun setupDatePicker(view: androidx.appcompat.widget.AppCompatEditText) {
        view.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(selectedYear, selectedMonth, selectedDay)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    view.setText(sdf.format(selectedDate.time))
                },
                year, month, day
            )

            datePickerDialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
