package com.example.marsphotos.ui.screens

import FirebaseViewModel
import PicsumImage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import kotlin.random.Random

class ImageViewModel( private val firebaseViewModel: FirebaseViewModel = FirebaseViewModel() ) : ViewModel() {
    private val picsumService = PicsumService.create()
    var images: List<PicsumImage> = emptyList()
        private set

    var selectedImageUrl: String? = null
        private set

    var imageCount: Int = 0
        private set

    var currentImageUrl by mutableStateOf<String?>(null)
        private set

    init {
        // Observe the last saved image and update currentImageUrl when it changes
        firebaseViewModel.lastSavedPicsumImage.observeForever { image ->
            image?.let { currentImageUrl = it.url }
        }
    }

    fun loadLastSavedImage() {
        firebaseViewModel.loadLastSavedImage("Picsum")
    }

    fun savePicsumPhoto(url: String) {
        firebaseViewModel.savePhotoToFirebase(
            url = url,
            source = "Picsum"
        )
    }


    fun fetchRandomImage() {
        viewModelScope.launch {
            images = picsumService.getImageList()
            imageCount = images.size

            // Select a random image URL from the retrieved list
            selectedImageUrl = if (images.isNotEmpty()) {
                images[Random.nextInt(images.size)].download_url
            } else {
                null
            }
        }
    }


}
