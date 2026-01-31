package us.mikeandwan.photos.api

import javax.inject.Inject
import retrofit2.Retrofit

class ConfigApiClient
    @Inject
    constructor(
        retrofit: Retrofit,
    ) : BaseApiClient() {
        private val _configApi: ConfigApi by lazy { retrofit.create(ConfigApi::class.java) }

        suspend fun getAccountStatus(): ApiResult<AccountStatus> =
            makeApiCall(
                ::getAccountStatus.name,
                suspend {
                    _configApi.getAccountStatus()
                },
            )

        suspend fun getScales(): ApiResult<List<Scale>> =
            makeApiCall(::getScales.name, suspend { _configApi.getScales() })
    }
