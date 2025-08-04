package us.mikeandwan.photos.domain

import androidx.room.withTransaction
import us.mikeandwan.photos.api.ConfigApiClient
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.ScaleDao
import javax.inject.Inject

class ConfigRepository @Inject constructor(
    private val api: ConfigApiClient,
    private val db: MawDatabase,
    private val scaleDao: ScaleDao,
    private val apiErrorHandler: ApiErrorHandler
)
{
    companion object {
        private const val ERR_MSG_LOAD_SCALES = "Unable to load configuration data at this time.  Please try again later."
    }

    fun loadScales() = api.loadData(
        apiCall = { api.getScales() },
        onSuccess = { scales ->
            val dbScales = scales.map { s -> s.toDatabaseScale() }

            db.withTransaction {
                scaleDao.upsert(*dbScales.toTypedArray())
            }

            scales
        },
        errorMessage = ERR_MSG_LOAD_SCALES,
        apiErrorHandler
    )

    fun us.mikeandwan.photos.api.Scale.toDatabaseScale(): us.mikeandwan.photos.database.Scale {
        return us.mikeandwan.photos.database.Scale(
            this.id,
            this.code,
            this.width,
            this.height,
            this.fillsDimensions
        )
    }
}
