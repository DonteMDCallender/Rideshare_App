package com.capstone.btmaprideshare

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.IOException
import java.util.*

public class MainActivity:
     AppCompatActivity() {

    private var imageView: ImageView? = null
    private var filePath : Uri? =null
    private var PICK_IMAGE_REQUEST = 71

    var storage: FirebaseStorage? =null
    var storageReference: StorageReference? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val btnUpload : Button? =findViewById(R.id.btnUpload)
        val btnChoose : Button? =findViewById(R.id.btnChoose)
        imageView = findViewById(R.id.imgView)

        btnChoose!!.setOnClickListener(View.OnClickListener { _: View? -> chooseImage() })

        btnUpload!!.setOnClickListener { _: View? -> uploadImage() }


        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.getReferenceFromUrl("gs://btmap-c029b.appspot.com")
    }

    private fun chooseImage() {
        val intent : Intent = Intent()
        intent.type = "images/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && requestCode == Activity.RESULT_OK && data != null && data.data != null) {
            filePath = data.data
            try {
                 val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                imageView?.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImage() {
        if (filePath != null) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            val ref =
                storageReference!!.child("images/" + UUID.randomUUID().toString())
            ref.putFile(filePath!!)
                .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot? ->
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Uploaded", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e: Exception ->
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Failed " + e.message, Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnProgressListener { snapshot: UploadTask.TaskSnapshot ->
                    val progress =
                        100.0 * snapshot.bytesTransferred / snapshot.totalByteCount
                    progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
                }
        }
    }
}

