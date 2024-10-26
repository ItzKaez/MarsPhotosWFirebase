package com.example.marsphotos.ui.screens

import PicsumImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import kotlin.random.Random

class ImageViewModel : ViewModel() {
    private val picsumService = PicsumService.create()
    var images: List<PicsumImage> = emptyList()
        private set

    var selectedImageUrl: String? = null
        private set

    var imageCount: Int = 0
        private set

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
