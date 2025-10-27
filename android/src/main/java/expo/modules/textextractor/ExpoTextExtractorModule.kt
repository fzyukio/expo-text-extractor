package expo.modules.textextractor

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.File

class ExpoTextExtractorModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoTextExtractor")

    Constants(
      "isSupported" to true
    )

    AsyncFunction("checkAvailability") { promise: Promise ->
      try {
        // Check if Google Play Services is available
        val context = appContext.reactContext!!
        val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        
        println("ExpoTextExtractor: Google Play Services check result: $resultCode")
        
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
          // Google Play Services not available (e.g., AVD with Google API)
          println("ExpoTextExtractor: Google Play Services not available, returning playServicesUnavailable: true")
          promise.resolve(mapOf("available" to false, "downloading" to false, "playServicesUnavailable" to true))
          return@AsyncFunction
        }
        
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        // Try to process a tiny dummy image to check if module is ready
        val dummyBitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        val inputImage = InputImage.fromBitmap(dummyBitmap, 0)
        
        recognizer.process(inputImage)
          .addOnSuccessListener {
            promise.resolve(mapOf("available" to true, "downloading" to false, "playServicesUnavailable" to false))
          }
          .addOnFailureListener { error ->
            if (error.message?.contains("Waiting for the text optional module to be downloaded") == true) {
              promise.resolve(mapOf("available" to false, "downloading" to true, "playServicesUnavailable" to false))
            } else {
              promise.resolve(mapOf("available" to false, "downloading" to false, "playServicesUnavailable" to false))
            }
          }
      } catch (error: Exception) {
        promise.resolve(mapOf("available" to false, "downloading" to false, "playServicesUnavailable" to true))
      }
    }

    AsyncFunction("extractTextFromImage") { uriString: String, promise: Promise ->
      try {
        val context = appContext.reactContext!!
        println("ExpoTextExtractor: Starting text extraction for URI: $uriString")
        
        val uri = if (uriString.startsWith("content://")) {
          Uri.parse(uriString)
        } else {
          val file = File(uriString)
          if (!file.exists()) {
            throw Exception("File not found: $uriString")
          }
          Uri.fromFile(file)
        }

        println("ExpoTextExtractor: Created URI: $uri")
        val inputImage = InputImage.fromFilePath(context, uri)
        println("ExpoTextExtractor: Created InputImage successfully")
        
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        println("ExpoTextExtractor: Created TextRecognizer successfully")

        recognizer.process(inputImage)
          .addOnSuccessListener { visionText ->
            val recognizedTexts = visionText.textBlocks.map { it.text }
            println("ExpoTextExtractor: Text recognition successful, found ${recognizedTexts.size} text blocks")
            promise.resolve(recognizedTexts)
          }
          .addOnFailureListener { error ->
            println("ExpoTextExtractor: Text recognition failed: ${error.message}")
            if (error.message?.contains("Waiting for the text optional module to be downloaded") == true) {
              promise.reject(CodedException("MODULE_DOWNLOAD_REQUIRED", "ML Kit text recognition module is downloading. Please wait and try again in a few moments.", error))
            } else {
              promise.reject(CodedException("TEXT_RECOGNITION_FAILED", "ML Kit text recognition failed: ${error.message}", error))
            }
          }
      } catch (error: Exception) {
        promise.reject(CodedException("UNKNOWN_ERROR", error.message ?: "Unknown error", error))
      }
    }
  }
}
