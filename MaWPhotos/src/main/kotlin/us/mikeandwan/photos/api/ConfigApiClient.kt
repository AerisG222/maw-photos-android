package us.mikeandwan.photos.api

import retrofit2.Retrofit
import javax.inject.Inject

class ConfigApiClient @Inject constructor(
    retrofit: Retrofit
): BaseApiClient() {
    private val _configApi: ConfigApi by lazy { retrofit.create(ConfigApi::class.java) }

    suspend fun getAccountStatus(): ApiResult<AccountStatus> {
        return makeApiCall(::getAccountStatus.name, suspend { _configApi.getAccountStatus() })
    }

    suspend fun getScales(): ApiResult<List<Scale>> {
        return makeApiCall(::getScales.name, suspend { _configApi.getScales() })
    }
}
