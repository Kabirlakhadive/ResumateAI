package com.example.buildairesume.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.buildairesume.databinding.FragmentAddAchievementBinding
import com.example.buildairesume.models.Achievement
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddAchievementFragment(
    private val onAdd: (Achievement) -> Unit,
    private val onEdit: ((Achievement, Int) -> Unit)? = null, // Edit callback
    private val achievementToEdit: Achievement? = null, // Existing achievement
    private val position: Int? = null
) : DialogFragment() {

    private var _binding: FragmentAddAchievementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAchievementBinding.inflate(inflater, container, false)
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
        handleCalendar()

        achievementToEdit?.let {
            binding.etTitle.setText(it.title)
            binding.etDate.setText(it.date)
            binding.btnAdd.text = "Update"
        }

        binding.btnAdd.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val date = binding.etDate.text.toString()

            if (title.isNotEmpty() && date.isNotEmpty()) {
                val newAchievement = Achievement(
                    achievementId = achievementToEdit?.achievementId, // Keep existing ID if editing
                    title = title,
                    date = date
                )
                if (achievementToEdit != null && position != null) {
                    onEdit?.invoke(newAchievement, position)
                } else {
                    onAdd(newAchievement)
                }
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun handleCalendar() {
        val date = binding.etDate
        date.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(selectedYear, selectedMonth, selectedDay)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    date.setText(sdf.format(selectedDate.time))
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
