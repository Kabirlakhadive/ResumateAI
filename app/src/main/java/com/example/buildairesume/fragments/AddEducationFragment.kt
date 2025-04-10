package com.example.buildairesume.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.buildairesume.BranchList
import com.example.buildairesume.DegreeList
import com.example.buildairesume.databinding.FragmentAddEducationBinding
import com.example.buildairesume.models.Education

class AddEducationFragment(
    private val onAdd: (Education) -> Unit,
    private val onEdit: ((Education, Int) -> Unit)? = null, // Edit callback
    private val educationToEdit: Education? = null, // Existing education
    private val position: Int? = null
) : DialogFragment() {

    private var _binding: FragmentAddEducationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEducationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()

        // Trigger UI update when qualification is selected
        binding.spinnerQualification.setOnItemClickListener { _, _, _, _ ->
            adjustFragment()
        }

        // Pre-fill data if editing
        educationToEdit?.let {
            binding.spinnerQualification.setText(it.qualification)
            binding.etSchool.setText(it.school)
            binding.etBoard.setText(it.board)
            binding.etDegree.setText(it.degree)
            binding.etBranch.setText(it.branch)
            binding.etSpecialization.setText(it.specialization)
            binding.spinnerGradingSystem.setText(it.gradingSystem)
            binding.etScore.setText(it.score)
            binding.etYearGraduation.setText(it.year)
            binding.btnAdd.text = "Update"

            // Adjust UI if editing existing Senior Secondary entry
            adjustFragment()
        }

        binding.btnAdd.setOnClickListener {
            val qualification = binding.spinnerQualification.text.toString()
            val school = binding.etSchool.text.toString()
            val board = binding.etBoard.text.toString()
            val degree = binding.etDegree.text.toString()
            val branch = binding.etBranch.text.toString()
            val specialization = binding.etSpecialization.text.toString()
            val gradingSystem = binding.spinnerGradingSystem.text.toString()
            val score = binding.etScore.text.toString()
            val year = binding.etYearGraduation.text.toString()

            if (qualification.isNotEmpty() && school.isNotEmpty() && board.isNotEmpty() &&
                gradingSystem.isNotEmpty() && score.isNotEmpty() && year.isNotEmpty()
            ) {
                val newEducation = Education(
                    educationId = educationToEdit?.educationId, // Keep ID if editing
                    qualification = qualification,
                    school = school,
                    board = board,
                    degree = degree,
                    branch = branch,
                    specialization = specialization,
                    gradingSystem = gradingSystem,
                    score = score,
                    year = year
                )

                if (educationToEdit != null && position != null) {
                    onEdit?.invoke(newEducation, position)
                } else {
                    onAdd(newEducation)
                }
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun adjustFragment() {
        if (binding.spinnerQualification.text.toString() == "12th Grade") {
            binding.fDegree.visibility = View.VISIBLE
            binding.fDegree.hint = "Stream"
            setupDropdown(
                binding.etDegree,
                listOf("Science", "Commerce", "Arts", "Vocational", "Humanities")
            )

            binding.fSchool.visibility = View.VISIBLE
            binding.fSchool.hint = "School"

            binding.fBoard.visibility = View.VISIBLE
            binding.fBoard.hint = "Board"

            // Hide Branch & Specialization when selecting Senior Secondary
            binding.fBranch.visibility = View.GONE
            binding.fSpecialization.visibility = View.GONE
        } else if (binding.spinnerQualification.text.toString() == "Diploma") {
            binding.fDegree.visibility = View.VISIBLE
            binding.fDegree.hint = "Diploma Title"
            setupDropdown(
                binding.etDegree,
                listOf(
                    "Diploma in Computer Engineering",
                    "Diploma in Mechanical Engineering",
                    "Diploma in Civil Engineering",
                    "Diploma in Electrical Engineering",
                    "Diploma in Electronics",
                    "Other"
                )
            )

            binding.fSchool.visibility = View.VISIBLE
            binding.fSchool.hint = "Institute/College"

            binding.fBoard.visibility = View.VISIBLE
            binding.fBoard.hint = "Board/University"

            binding.fBranch.visibility = View.VISIBLE
            binding.fBranch.hint = "Branch"

            // Specialization often not required for diploma
            binding.fSpecialization.visibility = View.GONE

        } else {
            binding.fDegree.visibility = View.VISIBLE
            binding.fDegree.hint = "Degree"
            setupDropdown(binding.etDegree, DegreeList.degrees)

            binding.fSchool.visibility = View.VISIBLE
            binding.fSchool.hint = "College"

            binding.fBoard.visibility = View.VISIBLE
            binding.fBoard.hint = "University"

            binding.fBranch.visibility = View.VISIBLE
            binding.fBranch.hint = "Branch"

            binding.fSpecialization.visibility = View.VISIBLE
            binding.fSpecialization.hint = "Specialization"
        }
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

    private fun setupSpinners() {
        setupDropdown(
            binding.spinnerQualification,
            listOf("12th Grade", "Under Graduate", "Post Graduate", "PhD", "Diploma")
        )
        setupDropdown(binding.spinnerGradingSystem, listOf("Percentage", "CGPA", "Percentile"))
        setupDropdown(binding.etDegree, DegreeList.degrees)
        setupDropdown(binding.etBranch, BranchList.Branches)
    }

    private fun setupDropdown(
        view: androidx.appcompat.widget.AppCompatAutoCompleteTextView,
        items: List<String>
    ) {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        view.setAdapter(adapter)
        view.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) view.showDropDown() }
        view.setOnClickListener { view.showDropDown() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
