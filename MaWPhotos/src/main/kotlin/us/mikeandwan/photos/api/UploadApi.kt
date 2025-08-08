package us.mikeandwan.photos.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

internal interface UploadApi {
    @Multipart
    @POST("upload/")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<FileOperationResult>
}
