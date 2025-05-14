package com.istentoumis.myqrscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoadPdfViewModel : ViewModel() {

    private val _authState = MutableLiveData<FirebaseUser?>()
    val authState: LiveData<FirebaseUser?> get() = _authState

    fun scanPDFForQRCodes(context: Context, pdfUri: Uri, uid: String, onQRCodeScanned: (String?) -> Unit) {
        try {
            val inputStream = context.contentResolver.openInputStream(pdfUri)
            val document = PDDocument.load(inputStream)
            val renderer = PDFRenderer(document)

            for (i in 0 until document.numberOfPages) {
                val pageBitmap = renderer.renderImageWithDPI(i, 300f)
                val (qrCodeContent) = decodeQRCode(pageBitmap)
                onQRCodeScanned(qrCodeContent)
            }
            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to scan PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun decodeQRCode(bitmap: Bitmap): Pair<String?, Rect?> {
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                com.google.zxing.BarcodeFormat.QR_CODE,
                com.google.zxing.BarcodeFormat.CODE_128,
                com.google.zxing.BarcodeFormat.CODE_39,
                com.google.zxing.BarcodeFormat.EAN_13,
                com.google.zxing.BarcodeFormat.UPC_A
            )
        )
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
            .addOnSuccessListener { Log.d("RealtimeDB", "QR Code content stored successfully.") }
            .addOnFailureListener { exception -> Log.e("RealtimeDB", "Failed to store QR Code content.", exception) }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
