import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class PicsumImage(val id: String, val download_url: String)

interface PicsumService {
    @GET("v2/list?page=0&limit=100") // Get a list of images
    suspend fun getImageList(): List<PicsumImage>

    companion object {
        private const val BASE_URL = "https://picsum.photos/"

        fun create(): PicsumService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PicsumService::class.java)
        }
    }
}