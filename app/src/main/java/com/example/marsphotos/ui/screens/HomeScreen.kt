/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.marsphotos.ui.screens

import FirebaseViewModel
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.marsphotos.R
import com.example.marsphotos.model.MarsPhoto
import com.example.marsphotos.ui.theme.MarsPhotosTheme
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects


@Composable
fun HomeScreen(
    marsUiState: MarsUiState,
    retryAction: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    when (marsUiState) {
        is MarsUiState.Loading -> LoadingScreen(modifier = modifier.fillMaxSize())
        is MarsUiState.Success -> ResultScreen(
            photos = marsUiState.photos,
            randomPhoto = marsUiState.randomPhoto,
            modifier = modifier.fillMaxWidth()
        )
        is MarsUiState.Error -> ErrorScreen(retryAction, modifier = modifier.fillMaxSize())
    }
}

/**
 * The home screen displaying the loading message.
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier.size(200.dp),
        painter = painterResource(R.drawable.loading_img),
        contentDescription = stringResource(R.string.loading)
    )
}

//Auxiliar function to create file name
fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val image = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir /* directory */
    )
    return image
}

fun uriToBase64(context: Context, uri: Uri): String {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val byteBuffer = ByteArrayOutputStream()
    val bufferSize = 1024
    val buffer = ByteArray(bufferSize)

    var len: Int
    while (inputStream?.read(buffer).also { len = it ?: -1 } != -1) {
        byteBuffer.write(buffer, 0, len)
    }

    val byteArray = byteBuffer.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

fun saveBase64ToFirebase(base64String: String, source: String) {
    val database = Firebase.database("https://marsphotos-f32fc-default-rtdb.europe-west1.firebasedatabase.app/")
    val photosRef = database.getReference("photos")

    val photoData = mapOf(
        "base64" to base64String,
        "source" to source,
        "timestamp" to System.currentTimeMillis()
    )

    photosRef.push().setValue(photoData)
}

@Composable
fun ResultScreen(
    photos: String,
    MarsViewModel: MarsViewModel = viewModel(),
    randomPhoto: MarsPhoto,
    modifier: Modifier = Modifier,
    imageViewModel: ImageViewModel = viewModel(),
    firebaseViewModel: FirebaseViewModel = viewModel()
) {
    var imageUrl by remember { mutableStateOf(imageViewModel.selectedImageUrl) }
    var imageCount by remember { mutableStateOf(imageViewModel.imageCount) }


    // Observe current image URLs
    var marsImageUrl by remember { mutableStateOf(randomPhoto.imgSrc) }
    val picsumImageUrl by remember { mutableStateOf(imageViewModel.currentImageUrl) }
    val rolls by remember { mutableStateOf(firebaseViewModel.rolls.value) }


    var save by remember { mutableStateOf("") }


    val context = LocalContext.current
    val file = context.createImageFile()
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        context.getPackageName() + ".provider", file
    )
    var capturedImageUri by remember {
        mutableStateOf<Uri>(Uri.EMPTY)
    }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            capturedImageUri = uri
        }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        cameraLauncher.launch(uri)
    }

    Column(
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(marsImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "A photo"
        )
        Text(text = photos)
        imageUrl?.let {
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentDescription = "Random Image",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        Text(text = "Total images retrieved: $imageCount")

        Text(text = "Number of rolls: ${rolls.toString()}")
        Text(text = save)


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                imageUrl = imageViewModel.selectedImageUrl + "?grayscale"
            }, modifier = Modifier.weight(1f)) {
                Text(text = "Gray", fontSize = 10.sp)
            }
            Button(onClick = {
                imageUrl = imageViewModel.selectedImageUrl + "?blur=5"
            }, modifier = Modifier.weight(1f)) {
                Text(text = "Blur", fontSize = 10.sp)
            }
            Button(onClick = {
                imageViewModel.fetchRandomImage()
                imageUrl = imageViewModel.selectedImageUrl
                imageCount = imageViewModel.imageCount
                MarsViewModel.getMarsPhotos()

                save = ""

                //save each roll number to firebase
                firebaseViewModel.updateCounter()
            }, modifier = Modifier.weight(1f)) {
                Text(text = "Roll", fontSize = 10.sp)
            }
            Button(onClick = {
                // Save the photo info to Firebase
                MarsViewModel.saveMarsPhoto(
                    url = randomPhoto.imgSrc
                )
                imageViewModel.savePicsumPhoto(
                    url = imageUrl ?: ""
                )
                save = "Saved"
                MarsViewModel.loadLastSavedImage()
                imageViewModel.loadLastSavedImage()
            }, modifier = Modifier.weight(1f)) {
                Text(text = "Save", fontSize = 10.sp)
            }
            Button(onClick = {
                imageUrl = picsumImageUrl
                marsImageUrl = MarsViewModel.currentImageUrl.toString()
            }, modifier = Modifier.weight(1f)) {
                Text(text = "Load", fontSize = 10.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        )

        {
            Button(onClick = {
                val permissionCheckResult =
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(uri)
                } else {
                    // Request a permission
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            }) {
                Text(text = "Photo")
            }
        }



    }
    if (capturedImageUri.path?.isNotEmpty() == true) {
        val context = LocalContext.current
        val base64String = uriToBase64(context, capturedImageUri)
        saveBase64ToFirebase(base64String, "Camera")
        AsyncImage(
            model = capturedImageUri, contentDescription = "photo", modifier = Modifier
                .padding(50.dp, 50.dp).fillMaxSize()
        )
    }
}



/**
 * The home screen displaying error message with re-attempt button.



 */
@Composable
fun ErrorScreen(retryAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_connection_error), contentDescription = ""
        )
        Text(text = stringResource(R.string.loading_failed), modifier = Modifier.padding(16.dp))
        Button(onClick = retryAction) {
            Text(stringResource(R.string.retry))
        }
    }
}

/**
 * The home screen displaying photo grid.
 */
@Composable
fun PhotosGridScreen(
    photos: List<MarsPhoto>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier.padding(horizontal = 4.dp),
        contentPadding = contentPadding,
    ) {
        items(items = photos, key = { photo -> photo.id }) { photo ->
            MarsPhotoCard(
                photo,
                modifier = modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
            )
        }
    }
}

@Composable
fun MarsPhotoCard(photo: MarsPhoto, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context = LocalContext.current).data(photo.imgSrc)
                .crossfade(true).build(),
            error = painterResource(R.drawable.ic_broken_image),
            placeholder = painterResource(R.drawable.loading_img),
            contentDescription = stringResource(R.string.mars_photo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    MarsPhotosTheme {
        LoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    MarsPhotosTheme {
        ErrorScreen({})
    }
}

@Preview(showBackground = true)
@Composable
fun PhotosGridScreenPreview() {
    MarsPhotosTheme {
        val mockData = List(10) { MarsPhoto("$it", "") }
        PhotosGridScreen(mockData)
    }
}
