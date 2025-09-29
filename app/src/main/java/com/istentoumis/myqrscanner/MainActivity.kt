package com.istentoumis.myqrscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.integration.android.IntentIntegrator
import com.istentoumis.myqrscanner.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import org.bouncycastle.asn1.x500.style.RFC4519Style.uid
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), LoginDialogFragment.LoginDialogListener {
    private val viewModel: MainViewModel by viewModels()
    @Inject
    lateinit var authManager: AuthManager
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private var progressDialog: ProgressDialogFragment? = null
    private lateinit var loginViewModel: LoginViewModel

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            viewModel.onCameraPermissionResult(isGranted)
        }

    private val openPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { pdfUri ->
                val uid = authManager.getUid()
                if (uid != null) {
                    viewModel.scanPDFForQRCodes(this, pdfUri, uid)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loginViewModel = LoginViewModel(authManager)
        observeViewModel()
        if (authManager.getCurrentUser() == null) {
            showLoginDialog()
        }
        binding.selectPdfCard.setOnClickListener {
            val uid = authManager.getUid()
            if (uid != null) {
                openFilePicker()
            } else {
                showLoginDialog()
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
        binding.logoutButton.setOnClickListener {
            authManager.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            showLoginDialog()
        }
        binding.selectCameraCard.setOnClickListener {
            if (uid != null) {
                checkCameraPermissionAndScan()
            } else {
                showLoginDialog()
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.cameraPermissionGranted.observe(this) { granted ->
            if (granted == true) {
                startQrScanner()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.qrScanResult.observe(this) { result ->
            result?.let {
                val uid = authManager.getUid()
                if (uid != null) {
                    showQRCodeResultDialog(it, uid)
                }
            }
        }
        viewModel.errorMessage.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun showLoginDialog() {
        LoginDialogFragment().show(supportFragmentManager, "LoginDialog")
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
        MaterialAlertDialogBuilder(this)
            .setTitle("QR Code Scan Result")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                qrCodeText?.let { showDescriptionDialog(it, uid) }
                dialog.dismiss()
            }
            .show()
    }

    private fun showDescriptionDialog(qrCodeContent: String, uid: String) {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle("Set QR Code Description")
        val input = EditText(this).apply { hint = "Enter description" }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                listOf("Cinema", "Gym", "Sports", "Other", "Ticket")
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(input)
            addView(spinner)
        }
        builder.setView(layout)
            .setPositiveButton("OK") { dialog, _ ->
                val description = input.text.toString()
                val type = spinner.selectedItem.toString()
                viewModel.storeQRCodeContent(uid, qrCodeContent, description, type)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
    }

    override fun onPhoneVerificationStarted(phone: String) {
        loginViewModel.startPhoneNumberVerification(this, phone)
    }

    private fun showVerificationCodeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_verification_code, null)
        val codeInput = dialogView.findViewById<TextInputEditText>(R.id.codeInput)
        MaterialAlertDialogBuilder(this)
            .setTitle("Enter Verification Code")
            .setView(dialogView)
            .setPositiveButton("Verify") { _, _ ->
                val code = codeInput.text.toString()
                if (code.isNotEmpty()) {
                    loginViewModel.verifyCode(this, code)
                } else {
                    Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.onCameraPermissionResult(true)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera permission is needed to scan QR codes", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Permission denied and rationale should not be shown (user selected 'Don't ask again')
                val showRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                if (!showRationale) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Permission Required")
                        .setMessage("Camera permission is required to scan QR codes. Please enable it in app settings.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    private fun startQrScanner() {
        IntentIntegrator(this).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Scan a QR code")
            setCameraId(-1)
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            super.onActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                viewModel.handleQRCodeScanResult(result.contents)
            }
        }
    }
}

