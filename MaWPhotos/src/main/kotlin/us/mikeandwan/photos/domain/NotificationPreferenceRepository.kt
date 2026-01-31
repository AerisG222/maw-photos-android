package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.NotificationPreferenceDao
import us.mikeandwan.photos.domain.models.NotificationPreference

class NotificationPreferenceRepository
    @Inject
    constructor(
        private val dao: NotificationPreferenceDao,
    ) {
        companion object {
            private const val PREFERENCE_ID = 1
        }

        fun getDoNotify() =
            dao
                .getNotificationPreference(PREFERENCE_ID)
                .map { it.doNotify }

        fun getDoVibrate() =
            dao
                .getNotificationPreference(PREFERENCE_ID)
                .map { it.doVibrate }

        suspend fun setDoNotify(doNotify: Boolean) {
            setPreference { it.copy(doNotify = doNotify) }
        }

        suspend fun setDoVibrate(doVibrate: Boolean) {
            setPreference { it.copy(doVibrate = doVibrate) }
        }

        private fun getNotificationPreferences() =
            dao
                .getNotificationPreference(PREFERENCE_ID)
                .map { it.toDomainNotificationPreference() }

        private suspend fun setNotificationPreferences(pref: NotificationPreference) {
            val dbPref = us.mikeandwan.photos.database
                .NotificationPreference(PREFERENCE_ID, pref.doNotify, pref.doVibrate)

            dao.setNotificationPreference(dbPref)
        }

        private suspend fun setPreference(update: (pref: NotificationPreference) -> NotificationPreference) {
            val pref = getNotificationPreferences().first()

            setNotificationPreferences(update(pref))
        }
    }
