package us.mikeandwan.photos.api

import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import java.io.File
import javax.inject.Inject

class UploadApiClient @Inject constructor(
    retrofit: Retrofit
): BaseApiClient() {
    private val _uploadApi: UploadApi by lazy { retrofit.create(UploadApi::class.java) }

    // https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server
    suspend fun uploadFile(file: File): ApiResult<UploadedFile> {
        val type = getMediaTypeForFile(file)
        val requestFile = file.asRequestBody(type)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        return makeApiCall(::uploadFile.name, suspend { _uploadApi.uploadFile(body) })
    }
}
