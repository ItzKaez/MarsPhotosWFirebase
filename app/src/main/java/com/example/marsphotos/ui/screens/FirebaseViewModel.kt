import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.marsphotos.data.PhotoInfo
import com.example.marsphotos.ui.screens.HomeScreen
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseViewModel : ViewModel() {

    // LiveData to hold the most recent Mars and Picsum images
    private val _lastSavedMarsImage = MutableLiveData<PhotoInfo?>()
    val lastSavedMarsImage: LiveData<PhotoInfo?> get() = _lastSavedMarsImage

    private val _lastSavedPicsumImage = MutableLiveData<PhotoInfo?>()
    val lastSavedPicsumImage: LiveData<PhotoInfo?> get() = _lastSavedPicsumImage

    // number of rolls
    var rolls = MutableLiveData<Int>()

    fun savePhotoToFirebase(url: String, source: String) {
        val database = Firebase.database("https://marsphotos-f32fc-default-rtdb.europe-west1.firebasedatabase.app/")
        val photosRef = database.getReference("photos")

        val photoData = mapOf(
            "url" to url,
            "source" to source,  // to differentiate between Mars and Picsum
            "timestamp" to System.currentTimeMillis()
        )

        photosRef.push().setValue(photoData)
    }

    //save roll number to firebase
    fun updateCounter() {
        val database =
            Firebase.database("https://marsphotos-f32fc-default-rtdb.europe-west1.firebasedatabase.app/")
        val counterRef = database.getReference("counter")

        // Lire la valeur actuelle du compteur
        counterRef.get().addOnSuccessListener { snapshot ->
            val currentValue =
                snapshot.getValue(Int::class.java) ?: 0 // Valeur par défaut à 0 si null

            // Incrémenter le compteur
            val newValue = currentValue + 1

            // Mettre à jour la valeur dans la base de données
            counterRef.setValue(newValue).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseViewModel", "Compteur mis à jour avec succès")
                    rolls.value = newValue

                } else {
                    Log.e("FirebaseViewModel", "Erreur lors de la mise à jour du compteur")
                }
            }

        }.addOnFailureListener {
            Log.e("FirebaseViewModel", "Erreur lors de la récupération du compteur")
        }
    }


    fun loadLastSavedImage(source: String) {
        Log.d("FirebaseViewModel", "Loading last saved image for source: $source")
        val database = Firebase.database("https://marsphotos-f32fc-default-rtdb.europe-west1.firebasedatabase.app/")
        val photosRef = database.getReference("photos")

        photosRef.orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("FirebaseViewModel", "Data found for source: $source")
                        val images = snapshot.children.mapNotNull { it.getValue(PhotoInfo::class.java) }
                        val marsImage = images.lastOrNull { it.source == "Mars" }
                        val picsumImage = images.lastOrNull { it.source == "Picsum" }

                        _lastSavedMarsImage.value = marsImage
                        _lastSavedPicsumImage.value = picsumImage
                    } else {
                        Log.d("FirebaseViewModel", "No data found for source: $source")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseViewModel", "Error loading image: ${error.message}")
                }
            })
    }
}
