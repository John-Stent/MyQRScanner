package com.istentoumis.myqrscanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.istentoumis.myqrscanner.databinding.ActivityMainBinding
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private val PDF_REQUEST_CODE = 1001
    private val SIGN_IN_REQUEST_CODE = 1234
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PDFBoxResourceLoader.init(applicationContext)
        auth = FirebaseAuth.getInstance()

        FirebaseAuth.getInstance().firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        startPhoneNumberVerification("+306974868456") // Replace with your phone number
        binding.loadPdfButton.setOnClickListener {
            if (auth.currentUser != null) {
                openFilePicker()
            }
            else {
                Toast.makeText(this, "Please sign in first.", Toast.LENGTH_LONG).show()
            }
        }
        binding.verifyButton.setOnClickListener {
            val code = binding.codeEditText.text.toString()
            if (code.isNotEmpty()) {
                verifyPhoneNumberWithCode(code)
            } else {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                    Log.w("STENT", "Verification completed: ${credential.smsCode}")
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@MainActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.w("STENT", "Verification failed: ${e.message}")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@MainActivity.verificationId = verificationId
                    Log.w("STENT", "Code sent: $verificationId")
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyPhoneNumberWithCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sign-in successful", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Sign-in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, PDF_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { pdfUri ->
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    scanPDFForQRCodes(pdfUri, uid)
                }
            }
        } else if (requestCode == SIGN_IN_REQUEST_CODE && resultCode == RESULT_OK) {
            auth.currentUser?.let {
                // User successfully signed in
            } ?: Toast.makeText(this, "Sign-in failed. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun scanPDFForQRCodes(pdfUri: Uri, uid: String) {
        try {
            val inputStream = contentResolver.openInputStream(pdfUri)
            val document = PDDocument.load(inputStream)
            val renderer = PDFRenderer(document)

            for (i in 0 until document.numberOfPages) {
                val pageBitmap = renderer.renderImageWithDPI(i, 300f)
                val (qrCodeContent) = decodeQRCode(pageBitmap)

                if (qrCodeContent != null) {
                    showDescriptionDialog(qrCodeContent, uid)
                    Log.d("QR Code Found", "QR Code found on page $i")
                    break
                } else {
                    Log.d("QR Code Not Found", "No QR Code found on page $i")
                }
            }
            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to scan PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun storeQRCodeContent(content: String, uid: String, description: String) {
        val qrCodeData = hashMapOf(
            "content" to content,
            "timestamp" to getCurrentDate(),
            "description" to description
        )
        databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://myqr-5607b-default-rtdb.europe-west1.firebasedatabase.app")
        databaseRef.child("Users").child(uid).push()
            .setValue(qrCodeData)
            .addOnSuccessListener { Log.d("RealtimeDB", "QR Code content stored successfully.") }
            .addOnFailureListener { exception -> Log.e("RealtimeDB", "Failed to store QR Code content.", exception) }
    }

    private fun decodeQRCode(bitmap: Bitmap): Pair<String?, Rect?> {
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = com.google.zxing.RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(
            BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.EAN_13, BarcodeFormat.UPC_A))
        reader.setHints(hints)

        return try {
            val result = reader.decode(binaryBitmap)
            val qrCodeRect = result.resultPoints?.let { points ->
                val xMin = points.minOf { it.x.toInt() }
                val xMax = points.maxOf { it.x.toInt() }
                val yMin = points.minOf { it.y.toInt() }
                val yMax = points.maxOf { it.y.toInt() }
                Rect(xMin, yMin, xMax, yMax)
            }
            Pair(result.text, qrCodeRect)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    private fun showQRCodeResultDialog(qrCodeText: String?) {
        val message = qrCodeText?.let { "QR code found in the PDF:\n$it" } ?: "No QR codes found in the PDF."
        AlertDialog.Builder(this)
            .setTitle("QR Code Scan Result")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun showDescriptionDialog(qrCodeContent: String, uid: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set QR Code Description")

        val input = binding.descriptionEditText
        input.hint = "Enter description"
        val parent = input.parent as? ViewGroup
        parent?.removeView(input)

        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val description = input.text.toString()
            storeQRCodeContent(qrCodeContent, uid, description)
            showQRCodeResultDialog(qrCodeContent)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}