package com.istentoumis.myqrscanner

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.istentoumis.myqrscanner.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginDialogFragment : DialogFragment() {
    interface LoginDialogListener {
        fun onPhoneVerificationStarted(phone: String)
    }

    private val viewModel: LoginViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var authManager: AuthManager
    private var loginDialogListener: LoginDialogListener? = null
    private var progressDialog: ProgressDialogFragment? = null
    private var verificationDialog: AlertDialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        loginDialogListener = context as? LoginDialogListener
    }

    override fun onDetach() {
        super.onDetach()
        loginDialogListener = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentLoginBinding.inflate(layoutInflater)
        val phoneInput = binding.phoneInput
        val sendCodeButton = binding.sendCodeButton
        phoneInput.requestFocus()

        sendCodeButton.setOnClickListener {
            val phone = phoneInput.text?.toString()?.trim()
            if (!phone.isNullOrEmpty()) {
                viewModel.startPhoneNumberVerification(requireActivity(), phone)
            } else {
                Toast.makeText(requireContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.verificationEvent.observe(this) { event ->
            when (event) {
                is LoginViewModel.VerificationEvent.Started -> {
                    progressDialog = ProgressDialogFragment()
                    progressDialog?.show(parentFragmentManager, "ProgressDialog")
                }
                is LoginViewModel.VerificationEvent.CodeSent -> {
                    progressDialog?.dismiss()
                    showVerificationCodeDialog()
                }
                is LoginViewModel.VerificationEvent.Completed -> {
                    progressDialog?.dismiss()
                    verificationDialog?.dismiss()
                    Toast.makeText(requireContext(), "User created successfully", Toast.LENGTH_LONG).show()
                    dismiss()
                }
                is LoginViewModel.VerificationEvent.Failed -> {
                    progressDialog?.dismiss()
                    verificationDialog?.dismiss()
                    Toast.makeText(requireContext(), "Verification failed: ${event.error}", Toast.LENGTH_LONG).show()
                }
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun showVerificationCodeDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val codeInput = EditText(requireContext()).apply {
            hint = "Enter verification code"
        }
        layout.addView(codeInput)

        verificationDialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter Verification Code")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Verify") { _, _ ->
                val code = codeInput.text.toString()
                if (code.isNotEmpty()) {
                    viewModel.verifyCode(requireActivity(), code)
                } else {
                    Toast.makeText(requireContext(), "Please enter the verification code", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
