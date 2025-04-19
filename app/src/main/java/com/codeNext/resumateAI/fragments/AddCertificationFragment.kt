package com.codeNext.resumateAI.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.codeNext.resumateAI.databinding.FragmentAddCertificationBinding
import com.codeNext.resumateAI.models.Certification

class AddCertificationFragment(
    private val onAdd: (Certification) -> Unit,
    private val onEdit: ((Certification, Int) -> Unit)? = null, // Edit callback
    private val certificationToEdit: Certification? = null, // Existing certification
    private val position: Int? = null
) : DialogFragment() {

    private var _binding: FragmentAddCertificationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCertificationBinding.inflate(inflater, container, false)
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
        certificationToEdit?.let {
            binding.etName.setText(it.name)
            binding.etYear.setText(it.year)
            binding.btnAdd.text = "Update"
        }

        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text.toString()
            val year = binding.etYear.text.toString()

            if (name.isNotEmpty() && year.isNotEmpty()) {
                val newCertification = Certification(
                    certificationId = certificationToEdit?.certificationId, // Keep ID if editing
                    name = name,
                    year = year
                )

                if (certificationToEdit != null && position != null) {
                    onEdit?.invoke(newCertification, position)
                } else {
                    onAdd(newCertification)
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
