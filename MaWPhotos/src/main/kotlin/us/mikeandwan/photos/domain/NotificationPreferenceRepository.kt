package us.mikeandwan.photos.domain

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.datastore.select
import us.mikeandwan.photos.domain.models.NotificationPreference

@Singleton
class NotificationPreferenceRepository
    @Inject
    constructor(
        private val dataStore: DataStore<UserPreferences>,
    ) {
        fun getDoNotify() = dataStore.select { it.notification.doNotify }

        fun getDoVibrate() = dataStore.select { it.notification.doVibrate }

        suspend fun setDoNotify(doNotify: Boolean) {
            setPreference { it.copy(doNotify = doNotify) }
        }

        suspend fun setDoVibrate(doVibrate: Boolean) {
            setPreference { it.copy(doVibrate = doVibrate) }
        }

        private suspend fun setPreference(update: (pref: NotificationPreference) -> NotificationPreference) {
            dataStore.updateData { it.copy(notification = update(it.notification)) }
        }
    }
