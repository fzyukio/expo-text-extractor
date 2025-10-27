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


        AsyncFunction("extractTextFromImage") { uriString: String, promise: Promise ->
          try {
            val context = appContext.reactContext!!
            
            // Reject data URIs as they're not supported with bundled model
            if (uriString.startsWith("data:")) {
              throw Exception("data: URIs are not supported; write to a temp file or pass content:// / file://")
            }
            
            val inputImage = when {
              uriString.startsWith("content://") || uriString.startsWith("file://") -> {
                val uri = Uri.parse(uriString)
                InputImage.fromFilePath(context, uri)
              }
              else -> {
                val file = File(uriString)
                if (!file.exists()) {
                  throw Exception("File not found: $uriString")
                }
                InputImage.fromFilePath(context, Uri.fromFile(file))
              }
            }
            
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
              .addOnSuccessListener { visionText ->
                val recognizedTexts = visionText.textBlocks.map { it.text }
                promise.resolve(recognizedTexts)
              }
              .addOnFailureListener { error ->
                promise.reject(CodedException("TEXT_RECOGNITION_FAILED", "ML Kit text recognition failed: ${error.message}", error))
              }
          } catch (error: Exception) {
            promise.reject(CodedException("UNKNOWN_ERROR", error.message ?: "Unknown error", error))
          }
        }
  }
}
