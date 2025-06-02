package com.istentoumis.myqrscanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
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
//        showPhoneNumberDialog()
        displayUserInfo(view)
        val phoneInput = view.findViewById<TextInputEditText>(R.id.phoneInput)
        val sendCodeButton = view.findViewById<MaterialButton>(R.id.sendCodeButton)

        sendCodeButton.setOnClickListener {
            val phone = phoneInput.text?.toString()?.trim()
            if (!phone.isNullOrEmpty()) {
                viewModel.startPhoneNumberVerification(requireActivity(), phone, verificationCallbacks)
            } else {
                Toast.makeText(requireContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }

        val logoutButton = view.findViewById<MaterialButton>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            viewModel.authManager.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            // Optionally, clear UI or navigate to login screen
            view.findViewById<TextView>(R.id.phoneNumberTextView).text = "Phone: N/A"
            view.findViewById<TextView>(R.id.qrCodeCountTextView).text = "QR Codes: N/A"
        }
    }

    private fun displayUserInfo(view: View) {
        val phoneTextView = view.findViewById<TextView>(R.id.phoneNumberTextView)
        val qrCountTextView = view.findViewById<TextView>(R.id.qrCodeCountTextView)
        val user = viewModel.authManager.getCurrentUser()
        phoneTextView.text = "Phone: ${user?.phoneNumber ?: "N/A"}"
        val uid = user?.uid ?: return
        val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("Users/$uid/QRCodeData")
        dbRef.get().addOnSuccessListener { snapshot ->
            val count = snapshot.childrenCount
            qrCountTextView.text = "QR Codes: $count"
        }.addOnFailureListener {
            qrCountTextView.text = "QR Codes: N/A"
        }
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
