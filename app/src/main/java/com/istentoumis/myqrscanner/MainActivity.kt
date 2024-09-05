package com.istentoumis.myqrscanner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.istentoumis.myqrscanner.databinding.ActivityMainBinding
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream
import java.util.UUID


class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding

    private val PDF_REQUEST_CODE = 1001

    private lateinit var databaseRef: DatabaseReference
    private lateinit var tokensRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(applicationContext)

        // Load PDF button listener
        binding.loadPdfButton.setOnClickListener {
            openFilePicker()
        }
    }

    // Function to open the file picker for PDF selection
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/pdf" // We are filtering for PDF files
        startActivityForResult(intent, PDF_REQUEST_CODE)
    }

    // Handling the result of file picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { pdfUri ->
                // You now have the URI of the selected PDF file
                scanPDFForQRCodes(pdfUri)
            }
        }
    }
    // Function to scan the PDF for QR codes
    private fun scanPDFForQRCodes(pdfUri: Uri) {
        try {
            // Open the PDF document
            val inputStream = contentResolver.openInputStream(pdfUri)
            val document = PDDocument.load(inputStream)

            // Use PDFRenderer to render the pages into images
            val renderer = PDFRenderer(document)
            val qrCodeBitmap: Bitmap

// Iterate through each page
            for (i in 0 until document.numberOfPages) {
                // Render at 300 DPI for better quality
                val pageBitmap = renderer.renderImageWithDPI(i, 300f)
                val (foundBitmap, qrCodeRect) = decodeQRCode(pageBitmap) // Scan for QR code

                if (foundBitmap != null) {
                    qrCodeBitmap = cropToQRCode(foundBitmap, qrCodeRect)
                    uploadQRCodeImage(qrCodeBitmap) // Upload QR code image
                    showQRCodeImage(qrCodeBitmap) // Show the QR code image in the ImageView
                    Log.d("QR Code Found", "QR Code found on page $i")
                    break // Exit loop if a QR code is found
                }
            }

            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Function to decode QR/Bar codes from the Bitmap using ZXing
    private fun decodeQRCode(bitmap: Bitmap): Pair<Bitmap?, Rect?> {
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = com.google.zxing.RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()

        // Specify the formats we want to detect (QR codes)
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.QR_CODE,          // QR code
                BarcodeFormat.CODE_128,         // 1D barcode
                BarcodeFormat.CODE_39,          // 1D barcode
                BarcodeFormat.EAN_13,           // 1D barcode
                BarcodeFormat.UPC_A             // 1D barcode
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
            Pair(bitmap, qrCodeRect) // Return the bitmap and QR code area
        } catch (e: Exception) {
            Pair(null, null) // No QR code found
        }
    }

    // Function to crop the bitmap to the QR code area with some padding
    private fun cropToQRCode(bitmap: Bitmap, rect: Rect?): Bitmap {
        return rect.let {
            // Add some padding to the rectangle
            val padding = 50 // Adjust padding as needed
            val left = (it!!.left - padding).coerceAtLeast(0)
            val top = (it.top - padding).coerceAtLeast(0)
            val right = (it.right + padding).coerceAtMost(bitmap.width)
            val bottom = (it.bottom + padding).coerceAtMost(bitmap.height)

            val width = right - left
            val height = bottom - top

            Bitmap.createBitmap(bitmap, left, top, width, height)
        }
    }

    // Convert Bitmap to Byte Array
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    // Function to upload the QR code image to Firebase Storage
    private fun uploadQRCodeImage(bitmap: Bitmap) {
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("qr_codes/${UUID.randomUUID()}.png")

        val byteArray = bitmapToByteArray(bitmap)

        imageRef.putBytes(byteArray)
            .addOnSuccessListener {
                Log.d("Upload", "QR Code image uploaded successfully.")
                // Optionally, store metadata in Firestore
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    storeQRCodeMetadata(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Upload", "Failed to upload QR Code image.", exception)
            }
    }
    // Function to store QR code metadata in Firebase Realtime Database
    private fun storeQRCodeMetadata(imageUrl: String) {
        val qrCodeData = hashMapOf(
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis() // Add any other metadata you need
        )
        databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://myqr-5607b-default-rtdb.europe-west1.firebasedatabase.app")

        // Generate a unique key for each QR code
        val newKey = databaseRef.push().key ?: return

        databaseRef.child(newKey)
            .setValue(qrCodeData)
            .addOnSuccessListener {
                Log.d("RealtimeDB", "QR Code metadata stored successfully.")
            }
            .addOnFailureListener { exception ->
                Log.e("RealtimeDB", "Failed to store QR Code metadata.", exception)
            }
    }
    // Function to show the QR code image in the ImageView
    private fun showQRCodeImage(bitmap: Bitmap) {
        binding.qrCodeImageView.setImageBitmap(bitmap)
        binding.qrCodeImageView.visibility = ImageView.VISIBLE
    }


    // Function to show the result in a dialog
    private fun showQRCodeResultDialog(qrCodeText: String?) {
        val message =
            if (qrCodeText != null) {
            "QR code found in the PDF:\n$qrCodeText"
        } else {
            "No QR codes found in the PDF."
        }


        AlertDialog.Builder(this)
            .setTitle("QR Code Scan Result")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()

    }
    // when the app resumes
    override fun onResume() {
        super.onResume()
    }

    // just before the app is paused
    override fun onPause() {
        super.onPause()
    }

//    private fun initiateScan()   {
//        val scannerView = binding.codeScannerView
//        codeScanner = CodeScanner(this, scannerView)
//
//        // The default values
//        codeScanner?.camera = CodeScanner.CAMERA_BACK
//        codeScanner?.formats = CodeScanner.ALL_FORMATS
//
//        codeScanner?.apply {
//            isAutoFocusEnabled = true
//            isFlashEnabled = false
//            autoFocusMode = AutoFocusMode.SAFE
//            scanMode = ScanMode.SINGLE
//        }
//
//        // this is called after the code is captured
//        codeScanner!!.decodeCallback = DecodeCallback {
//            runOnUiThread {
//
//                // Read from the database
//                tokensRef.addListenerForSingleValueEvent(object: ValueEventListener {
//
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        if(snapshot.exists() && snapshot.hasChild(it.text.toString())){
//                            val timestamp:Long  = snapshot.child(it.text.toString()).child("timestamp").value as Long
//                            if ( timestamp > (System.currentTimeMillis() - 30000)){ // token exists for less than 30 seconds
//                                val userId = snapshot.child(it.text.toString()).child("userID").value
//
//                                //starting MessageActivity
//                                val intent = Intent(this@MainActivity, MessageActivity::class.java)
//                                intent.putExtra("token", it.text.toString())
//                                intent.putExtra("userID", userId.toString())
//                                intent.putExtra("invalid", "no")
//                                startActivity(intent)
//                            }
//                            else{
//                                Toast.makeText(this@MainActivity, "Database access denied. " +
//                                        "Please try again with another QR",Toast.LENGTH_SHORT).show()
//
//                                val mIntent = intent
//                                finish()
//                                startActivity(mIntent)
//                            }
//
//                        }
//                        else{
//                            //Snackbar.make(scannerView, "There is no such user", 5000).show()
//
//                            //starting MessageActivity
//                            val intent = Intent(this@MainActivity, MessageActivity::class.java)
//                            intent.putExtra("token", "null")
//                            intent.putExtra("userID", "null")
//                            intent.putExtra("invalid", "yes")
//                            startActivity(intent)
//                        }
//                    }
//                    override fun onCancelled(error: DatabaseError) {
//                        Log.w(TAG, "Failed to read value.", error.toException())
//                        Toast.makeText(this@MainActivity, "Database access denied. Please try again with another QR",Toast.LENGTH_LONG).show()
//
//                        val mIntent = intent
//                        finish()
//                        startActivity(mIntent)
//                    }
//                })
//
//
//                //Snackbar.make(scannerView, "Scan result: ${it.text}", 5000).show()
//            }
//        }
//
//        // Whenever an error occurs when decoding
//        codeScanner?.errorCallback = ErrorCallback {
//            runOnUiThread {
//                Toast.makeText(
//                    this, "Camera initialization error: ${it.message}",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//
//        // Restart scan process when the scanner is tapped
//        scannerView.setOnClickListener {
//            codeScanner!!.startPreview()
//        }
//    }
}