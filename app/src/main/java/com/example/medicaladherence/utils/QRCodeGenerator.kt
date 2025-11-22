package com.example.medicaladherence.utils

import android.graphics.Bitmap
import android.graphics.Color

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRCodeGenerator {
    
    /**
     * Generate QR code bitmap from string data
     * @param data The string data to encode in QR code
     * @param size The size of the QR code in pixels (width and height)
     * @return Bitmap of the QR code or null if generation fails
     */
    fun generateQRCode(data: String, size: Int = AppConstants.QR_CODE_SIZE): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }
            
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(
                data,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("QRCodeGenerator", "Failed to generate QR code: ${e.message}", e)
            null
        }
    }
    
    /**
     * Compress data if it's too large for a QR code
     * QR codes have a maximum capacity of ~4,296 alphanumeric characters
     */
    fun isDataTooLarge(data: String): Boolean {
        return data.length > AppConstants.QR_CODE_MAX_LENGTH
    }
}

