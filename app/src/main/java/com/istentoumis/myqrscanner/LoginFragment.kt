package com.istentoumis.myqrscanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showPhoneNumberDialog()
    }

    private fun showPhoneNumberDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Login")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL

        val phoneInput = EditText(requireContext())
        phoneInput.hint = "Enter phone number"
        layout.addView(phoneInput)

        builder.setView(layout)

        builder.setPositiveButton("Send Code") { _, _ ->
            val phoneNumber = phoneInput.text.toString()
            if (phoneNumber.isNotEmpty()) {
                viewModel.startPhoneNumberVerification(requireActivity(), phoneNumber, verificationCallbacks)
            } else {
                Toast.makeText(requireContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            viewModel.signInWithPhoneAuthCredential(requireActivity(), credential) { success, message ->
                if (success) {
                    Toast.makeText(requireContext(), "Sign-in successful", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Sign-in failed: $message", Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            viewModel.authManager.setVerificationId(verificationId)
            showVerificationCodeDialog(verificationId)
        }
    }

    private fun showVerificationCodeDialog(verificationId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Verification Code")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL

        val codeInput = EditText(requireContext())
        codeInput.hint = "Enter verification code"
        layout.addView(codeInput)

        builder.setView(layout)

        builder.setPositiveButton("Verify") { _, _ ->
            val code = codeInput.text.toString()
            if (code.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                viewModel.createUserWithPhoneNumber(credential) { success, message ->
                    if (success) {
                        Toast.makeText(requireContext(), "User created successfully", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to create user: $message", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please enter the verification code", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}
