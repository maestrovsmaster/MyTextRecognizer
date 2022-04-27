package com.maestrovs.mytextrecognizer

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class MainActivity : AppCompatActivity() {

    lateinit var recognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        btTakePhoto.setOnClickListener {
            ImagePicker.with(this)
                .compress(1024)         //Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)  //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent { intent ->
                    progress.visibility = View.VISIBLE
                    startForProfileImageResult.launch(intent)
                }
        }


    }



    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            if (resultCode == Activity.RESULT_OK) {
                //Image Uri will not be null for RESULT_OK
                val fileUri = data?.data!!
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, fileUri))
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, fileUri)
                }
                setImage(bitmap)

                val image: InputImage
                try {
                    image = InputImage.fromFilePath(this@MainActivity, fileUri)
                    processImage(image)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.d("MyRecognizedText","InputImage err = ${e.localizedMessage}")
                    tvResult.text = e.localizedMessage
                }


            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }


    fun setImage(bitmap: Bitmap){
        imageView.setImageBitmap(bitmap)
    }

    fun processImage(image:InputImage){

        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                progress.visibility = View.GONE
                Log.d("MyRecognizedText","text = ${visionText.text}")
                tvResult.text = visionText.text

                calculateWords(visionText.text)
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                Log.d("MyRecognizedText","err = ${e.localizedMessage}")
                tvResult.text = e.localizedMessage
            }
    }

    fun calculateWords(text: String){
        val list = text.split(" ")

        val myPairs = hashMapOf<String, Int>()

        list.map { key ->
            if(myPairs.containsKey(key)){
                val currentCnt = myPairs.get(key)
                currentCnt?.let {
                    myPairs.set(key, it+1)
                }
            }else{
                myPairs.set(key, 1)
            }
        }

        var formattedOut = ""

        myPairs.map {
            formattedOut += "${it.key} = ${it.value} \n"
        }

        tvResult2.text = formattedOut

    }

}


