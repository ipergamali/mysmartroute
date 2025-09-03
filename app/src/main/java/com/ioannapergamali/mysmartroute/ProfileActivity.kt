package com.ioannapergamali.mysmartroute

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ioannapergamali.mysmartroute.databinding.ActivityProfileBinding
import java.util.UUID

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            binding.ivProfile.setImageURI(it)
            uploadImageToFirebase(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectImage.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        loadExistingPhoto()
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val storageRef = Firebase.storage.reference
            .child("profileImages/${UUID.randomUUID()}.jpg")

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                saveUrlToFirestore(downloadUri.toString())
            }
            .addOnFailureListener { e ->
                Log.e("Upload", "Αποτυχία ανεβάσματος", e)
            }
    }

    private fun saveUrlToFirestore(url: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("users")
            .document(uid)
            .set(mapOf("photoUrl" to url), SetOptions.merge())
    }

    private fun loadExistingPhoto() {
        val path = "profileImages/hcZBYbDteKWV8hrjnW9CqKfewFl2.jpg"
        Firebase.storage.reference.child(path).downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(binding.ivProfile)
            }
    }
}
