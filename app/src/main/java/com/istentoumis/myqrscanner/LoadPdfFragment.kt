package com.istentoumis.myqrscanner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoadPdfFragment : Fragment(R.layout.fragment_load_pdf) {

    private val viewModel: LoadPdfViewModel by viewModels()

    @Inject
    lateinit var authManager: AuthManager // Inject AuthManager using Hilt

    private val openPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { pdfUri ->
                val uid = authManager.getUid() // Retrieve the UID globally
                if (uid != null) {
                    viewModel.scanPDFForQRCodes(requireContext(), pdfUri, uid) { qrCodeContent ->
                        showQRCodeResultDialog(qrCodeContent, uid)
                    }
                } else {
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loadPdfButton: Button = view.findViewById(R.id.loadPdfButton)
        loadPdfButton.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        openPdfLauncher.launch(intent)
    }

    private fun showQRCodeResultDialog(qrCodeText: String?, uid: String) {
        val message = qrCodeText?.let { "QR code found in the PDF" } ?: "No QR codes found in the PDF."
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("QR Code Scan Result")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                if (qrCodeText != null) {
                    showDescriptionDialog(qrCodeText, uid)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showDescriptionDialog(qrCodeContent: String, uid: String) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Set QR Code Description")

        val input = android.widget.EditText(requireContext())
        input.hint = "Enter description"

        val spinner = android.widget.Spinner(requireContext())
        val types = listOf("Cinema", "Gym", "Sports", "Other", "Ticket")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(input)
            addView(spinner)
        }

        builder.setView(layout)
        builder.setPositiveButton("OK") { dialog, _ ->
            val description = input.text.toString()
            val type = spinner.selectedItem.toString()
            viewModel.storeQRCodeContent(uid, qrCodeContent, description, type)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}
