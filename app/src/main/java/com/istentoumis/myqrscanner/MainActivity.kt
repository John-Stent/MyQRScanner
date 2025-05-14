package com.istentoumis.myqrscanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Retrieve the NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController

        // Link the NavController to the BottomNavigationView
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        NavigationUI.setupWithNavController(bottomNavigationView, navController)
    }
}
//import android.app.AlertDialog
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.Rect
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.ViewGroup
//import android.widget.ArrayAdapter
//import android.widget.EditText
//import android.widget.LinearLayout
//import android.widget.Spinner
//import android.widget.Toast
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import com.google.firebase.FirebaseException
//import com.google.firebase.auth.PhoneAuthCredential
//import com.google.firebase.auth.PhoneAuthProvider
//import com.google.firebase.database.FirebaseDatabase
//import com.google.zxing.BarcodeFormat
//import com.google.zxing.BinaryBitmap
//import com.google.zxing.DecodeHintType
//import com.google.zxing.MultiFormatReader
//import com.google.zxing.RGBLuminanceSource
//import com.google.zxing.common.HybridBinarizer
//import com.google.zxing.integration.android.IntentIntegrator
//import com.google.zxing.integration.android.IntentIntegrator.QR_CODE
//import com.istentoumis.myqrscanner.databinding.ActivityMainBinding
//import com.tom_roush.pdfbox.pdmodel.PDDocument
//import com.tom_roush.pdfbox.rendering.PDFRenderer
//import dagger.hilt.android.AndroidEntryPoint
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class MainActivity : AppCompatActivity() {
//    private val PDF_REQUEST_CODE = 1001
//    private lateinit var binding: ActivityMainBinding
//    private val vm: MainViewModel by viewModels()
//    @Inject lateinit var authManager: AuthManager
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        vm.authState.observe(this) { user ->
//            if (user == null) {
//                showPhoneNumberDialog()
//            }
//        }
//
//        binding.loadPdfButton.setOnClickListener {
//            if (vm.authState.value != null) {
//                openFilePicker()
//            } else {
//                Toast.makeText(this, "Please sign in first.", Toast.LENGTH_LONG).show()
//                showPhoneNumberDialog()
//            }
//        }
//
//        binding.scanQrCodeButton.setOnClickListener {
//            if (vm.authState.value != null) {
//                val intentIntegrator = IntentIntegrator(this)
//                intentIntegrator.setDesiredBarcodeFormats(QR_CODE)
//                intentIntegrator.setPrompt("Scan a QR Code")
//                intentIntegrator.setCameraId(0)
//                intentIntegrator.setOrientationLocked(false)
//                intentIntegrator.setBeepEnabled(true)
//                intentIntegrator.setBarcodeImageEnabled(true)
//                intentIntegrator.initiateScan()
//            } else {
//                Toast.makeText(this, "Please sign in first.", Toast.LENGTH_LONG).show()
//                showPhoneNumberDialog()
//            }
//        }
//    }
//
//    private fun showPhoneNumberDialog() {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Login")
//
//        val layout = LinearLayout(this)
//        layout.orientation = LinearLayout.VERTICAL
//
//        val phoneInput = EditText(this)
//        phoneInput.hint = "Enter phone number"
//        layout.addView(phoneInput)
//
//        builder.setView(layout)
//
//        builder.setPositiveButton("Send Code") { _, _ ->
//            val phoneNumber = phoneInput.text.toString()
//            if (phoneNumber.isNotEmpty()) {
//                vm.startPhoneNumberVerification(this, phoneNumber, verificationCallbacks)
//            } else {
//                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        builder.setNegativeButton("Cancel") { dialog, _ ->
//            dialog.cancel()
//        }
//
//        builder.show()
//    }
//
//    private val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
//        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
//            vm.signInWithPhoneAuthCredential(this@MainActivity, credential) { success, message ->
//                if (success) {
//                    Toast.makeText(this@MainActivity, "Sign-in successful", Toast.LENGTH_LONG).show()
//                } else {
//                    Toast.makeText(this@MainActivity, "Sign-in failed: $message", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//
//        override fun onVerificationFailed(e: FirebaseException) {
//            Toast.makeText(this@MainActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//
//        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
//            authManager.setVerificationId(verificationId)
//            showVerificationCodeDialog()
//        }
//    }
//
//    private fun showVerificationCodeDialog() {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Enter Verification Code")
//
//        val layout = LinearLayout(this)
//        layout.orientation = LinearLayout.VERTICAL
//
//        val codeInput = EditText(this)
//        codeInput.hint = "Enter verification code"
//        layout.addView(codeInput)
//
//        builder.setView(layout)
//
//        builder.setPositiveButton("Verify") { _, _ ->
//            val code = codeInput.text.toString()
//            if (code.isNotEmpty()) {
//                val credential = authManager.verifyPhoneNumberWithCode(code)
//                vm.createUserWithPhoneNumber(credential) { success, message ->
//                    if (success) {
//                        Toast.makeText(this, "User created successfully", Toast.LENGTH_LONG).show()
//                    } else {
//                        Toast.makeText(this, "Failed to create user: $message", Toast.LENGTH_LONG).show()
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        builder.setNegativeButton("Cancel") { dialog, _ ->
//            dialog.cancel()
//        }
//
//        builder.show()
//    }
//
//    private fun openFilePicker() {
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "application/pdf"
//        }
//        startActivityForResult(intent, PDF_REQUEST_CODE)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PDF_REQUEST_CODE && resultCode == RESULT_OK) {
//            data?.data?.let { pdfUri ->
//                val uid = vm.authState.value?.uid
//                if (uid != null) {
//                    scanPDFForQRCodes(pdfUri, uid)
//                }
//            }
//        }
//        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
//        if (result != null) {
//            if (result.contents == null) {
//                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
//            } else {
//                val uid = vm.authState.value?.uid
//                if (uid != null) {
//                    showDescriptionDialog(result.contents, uid)
//                    Toast.makeText(this, "QR Code scanned: ${result.contents}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun scanPDFForQRCodes(pdfUri: Uri, uid: String) {
//        try {
//            val inputStream = contentResolver.openInputStream(pdfUri)
//            val document = PDDocument.load(inputStream)
//            val renderer = PDFRenderer(document)
//
//            for (i in 0 until document.numberOfPages) {
//                val pageBitmap = renderer.renderImageWithDPI(i, 300f)
//                val (qrCodeContent) = decodeQRCode(pageBitmap)
//                showQRCodeResultDialog(qrCodeContent, uid)
//            }
//            document.close()
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "Failed to scan PDF: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun storeQRCodeContent(content: String, uid: String, description: String, type: String) {
//        val qrCodeData = mapOf(
//            "content" to content,
//            "timestamp" to getCurrentDate(),
//            "description" to description,
//            "type" to type
//        )
//        val databaseRef = FirebaseDatabase.getInstance().getReference("Users/$uid/QRCodeData")
//        databaseRef.push()
//            .setValue(qrCodeData)
//            .addOnSuccessListener { Log.d("RealtimeDB", "QR Code content stored successfully.") }
//            .addOnFailureListener { exception -> Log.e("RealtimeDB", "Failed to store QR Code content.", exception) }
//    }
//
//    private fun decodeQRCode(bitmap: Bitmap): Pair<String?, Rect?> {
//        val intArray = IntArray(bitmap.width * bitmap.height)
//        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
//        val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
//        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
//        val reader = MultiFormatReader()
//        val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(
//            BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.EAN_13, BarcodeFormat.UPC_A))
//        reader.setHints(hints)
//
//        return try {
//            val result = reader.decode(binaryBitmap)
//            val qrCodeRect = result.resultPoints?.let { points ->
//                val xMin = points.minOf { it.x.toInt() }
//                val xMax = points.maxOf { it.x.toInt() }
//                val yMin = points.minOf { it.y.toInt() }
//                val yMax = points.maxOf { it.y.toInt() }
//                Rect(xMin, yMin, xMax, yMax)
//            }
//            Pair(result.text, qrCodeRect)
//        } catch (e: Exception) {
//            Pair(null, null)
//        }
//    }
//
//    private fun showQRCodeResultDialog(qrCodeText: String?, uid: String) {
//        val message = qrCodeText?.let { "QR code found in the PDF" } ?: "No QR codes found in the PDF."
//        AlertDialog.Builder(this)
//            .setTitle("QR Code Scan Result")
//            .setMessage(message)
//            .setPositiveButton("OK") { dialog, _ ->
//                if (qrCodeText != null) {
//                    showDescriptionDialog(qrCodeText, uid)
//                }
//                dialog.dismiss()}
//            .show()
//    }
//
//    private fun getCurrentDate(): String {
//        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//        return dateFormat.format(Date())
//    }
//
//    private fun showDescriptionDialog(qrCodeContent: String, uid: String) {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Set QR Code Description")
//
//        val layout = LinearLayout(this)
//        layout.orientation = LinearLayout.VERTICAL
//
//        val input = EditText(this)
//        input.hint = "Enter description"
//        val parent = input.parent as? ViewGroup
//        parent?.removeView(input)
//
//        val spinner = Spinner(this)
//        val types = listOf("Cinema", "Gym", "Sports", "Other", "Ticket")
//        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinner.adapter = adapter
//        layout.addView(spinner)
//        builder.setView(input)
//
//        builder.setPositiveButton("OK") { dialog, _ ->
//            val description = input.text.toString()
//            val type = spinner.selectedItem.toString()
//            storeQRCodeContent(qrCodeContent, uid, description,type)
//            dialog.dismiss()
//        }
//        builder.setNegativeButton("Cancel") { dialog, _ ->
//            dialog.cancel()
//        }
//
//        builder.show()
//    }
//}