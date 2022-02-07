package de.paulweber.routes.profile

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import java.io.ByteArrayOutputStream

object QRCodeGenerator {
    fun generate(toEncode: String): ByteArray {
        val encoded = MultiFormatWriter().encode(toEncode, BarcodeFormat.QR_CODE, 300, 300)
        val byteStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(encoded, "png", byteStream)
        return byteStream.toByteArray()
    }
}