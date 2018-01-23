package com.codility.qrcode.scannerpicture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

/**
 * Created by Govind on 1/23/2018.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "QRCode Scanner"
    private val REQUEST_WRITE_PERMISSION = 20
    private val PHOTO_REQUEST = 10
    private var barcodeDetector: BarcodeDetector? = null
    private var imageUri: Uri? = null
    private val SAVED_INSTANCE_URI = "uri"
    private val SAVED_INSTANCE_RESULT = "result"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            imageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI))
            tvResult!!.text = savedInstanceState.getString(SAVED_INSTANCE_RESULT)
        }

        btTakePicture.setOnClickListener(View.OnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSION)
        })

        barcodeDetector = BarcodeDetector.Builder(baseContext)
                .setBarcodeFormats(Barcode.DATA_MATRIX or Barcode.QR_CODE)
                .build()

        if (!barcodeDetector!!.isOperational) {
            tvResult!!.text = "Could not set up the barcodeDetector!"
            return
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_PERMISSION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PHOTO_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            mediaScanIntent()
            try {
                val bitmap = decodeBitmapUri(this, this.imageUri!!)
                if (barcodeDetector!!.isOperational && bitmap != null) {
                    val frame = Frame.Builder().setBitmap(bitmap).build()
                    val barCodes = barcodeDetector!!.detect(frame)
                    for (index in 0 until barCodes.size()) {
                        val code = barCodes.valueAt(index)
                        tvResult!!.text = tvResult!!.text.toString().plus(code.displayValue).plus("\n")
                        Log.e(TAG, tvResult!!.text.toString())
                        //Required only if you need to extract the type of barcode
                        val type = barCodes.valueAt(index).valueFormat
                        when (type) {
                            Barcode.CONTACT_INFO -> Log.e(TAG, code.contactInfo.title)
                            Barcode.EMAIL -> Log.e(TAG, code.email.address)
                            Barcode.ISBN -> Log.e(TAG, code.rawValue)
                            Barcode.PHONE -> Log.e(TAG, code.phone.number)
                            Barcode.PRODUCT -> Log.e(TAG, code.rawValue)
                            Barcode.SMS -> Log.e(TAG, code.sms.message)
                            Barcode.TEXT -> Log.e(TAG, code.rawValue)
                            Barcode.URL -> Log.e(TAG, "url: " + code.url.url)
                            Barcode.WIFI -> Log.e(TAG, code.wifi.ssid)
                            Barcode.GEO -> Log.e(TAG, code.geoPoint.lat.toString() + ":" + code.geoPoint.lng)
                            Barcode.CALENDAR_EVENT -> Log.e(TAG, code.calendarEvent.description)
                            Barcode.DRIVER_LICENSE -> Log.e(TAG, code.driverLicense.licenseNumber)
                            else -> Log.e(TAG, code.rawValue)
                        }
                    }
                    if (barCodes.size() == 0) {
                        tvResult!!.text = "Scan Failed: Found nothing to scan"
                    }
                } else {
                    tvResult!!.text = "Could not set up the barcodeDetector!"
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT).show()
                Log.e(TAG, e.toString())
            }
        }
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photo = File(Environment.getExternalStorageDirectory(), "photo.jpg")
        imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", photo)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, PHOTO_REQUEST)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        if (imageUri != null) {
            outState!!.putString(SAVED_INSTANCE_URI, imageUri.toString())
            outState.putString(SAVED_INSTANCE_RESULT, tvResult!!.text.toString())
        }
        super.onSaveInstanceState(outState)
    }

    private fun mediaScanIntent() {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = imageUri
        this.sendBroadcast(mediaScanIntent)
    }

    private fun decodeBitmapUri(ctx: Context, uri: Uri?): Bitmap {
        val targetW = 600
        val targetH = 600
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeStream(ctx.contentResolver.openInputStream(uri), null, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight
        val scaleFactor = Math.min(photoW / targetW, photoH / targetH)
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        return BitmapFactory.decodeStream(ctx.contentResolver.openInputStream(uri), null, bmOptions)
    }
}
