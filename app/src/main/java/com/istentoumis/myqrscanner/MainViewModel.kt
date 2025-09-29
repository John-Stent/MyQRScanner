package com.istentoumis.myqrscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _authState = MutableLiveData<FirebaseUser?>()
    val authState: LiveData<FirebaseUser?> get() = _authState

    private val _cameraPermissionGranted = MutableLiveData<Boolean>()
    val cameraPermissionGranted: LiveData<Boolean> get() = _cameraPermissionGranted

    private val _qrScanResult = MutableLiveData<String?>()
    val qrScanResult: LiveData<String?> get() = _qrScanResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun onCameraPermissionResult(isGranted: Boolean) {
        _cameraPermissionGranted.value = isGranted
        if (!isGranted) {
            _errorMessage.value = "Camera permission denied"
        }
    }

    fun handleQRCodeScanResult(result: String?) {
        if (result != null) {
            _qrScanResult.value = result
        } else {
            _errorMessage.value = "No QR code found"
        }
    }

    fun scanPDFForQRCodes(context: Context, pdfUri: Uri, uid: String) {
        try {
            val inputStream = context.contentResolver.openInputStream(pdfUri)
            val document = PDDocument.load(inputStream)
            val renderer = PDFRenderer(document)
            for (i in 0 until document.numberOfPages) {
                val page = document.getPage(i)
                val resources = page.resources
                for (xObjectName in resources.xObjectNames) {
                    val xObject = resources.getXObject(xObjectName)
                    if (xObject is com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                        val imageBitmap = xObject.image
                        val (qrCodeContent) = decodeQRCode(imageBitmap)
                        if (qrCodeContent != null) {
                            _qrScanResult.value = qrCodeContent
                            document.close()
                            return
                        }
                    }
                }
            }
            document.close()
            _errorMessage.value = "No QR codes found in the PDF."
        } catch (e: Exception) {
            _errorMessage.value = "Error scanning PDF: ${e.message}"
        }
    }

    fun decodeQRCode(bitmap: Bitmap): Pair<String?, Rect?> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            val result = MultiFormatReader().decode(binaryBitmap, mapOf(DecodeHintType.TRY_HARDER to true))
            val points = result.resultPoints
            val rect = if (points != null && points.isNotEmpty()) {
                val minX = points.minOf { it.x }.toInt()
                val minY = points.minOf { it.y }.toInt()
                val maxX = points.maxOf { it.x }.toInt()
                val maxY = points.maxOf { it.y }.toInt()
                Rect(minX, minY, maxX, maxY)
            } else {
                null
            }
            Pair(result.text, rect)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    fun storeQRCodeContent(uid: String, content: String, description: String, type: String) {
        val qrCodeData = mapOf(
            "content" to content,
            "timestamp" to getCurrentDate(),
            "description" to description,
            "type" to type
        )
        val databaseRef = FirebaseDatabase.getInstance().getReference("Users/$uid/QRCodeData")
        databaseRef.push()
            .setValue(qrCodeData)
            .addOnSuccessListener { Log.d("RealtimeDB", "QR Code content stored successfully.")
            }
            .addOnFailureListener { exception -> Log.e("RealtimeDB", "Failed to store QR Code content.", exception) }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}