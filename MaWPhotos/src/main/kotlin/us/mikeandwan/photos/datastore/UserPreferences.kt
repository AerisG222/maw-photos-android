package us.mikeandwan.photos.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.models.NotificationPreference
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.PlacePreference
import us.mikeandwan.photos.domain.models.RandomPreference
import us.mikeandwan.photos.domain.models.SearchPreference

/**
 * Every setting the app keeps, written to disk as one json document.
 *
 * A setting is added by giving its model a new property with a default - there is no schema to
 * migrate, and a document written before the property existed simply reads back the default.  The
 * property names are what is stored, though, so renaming one quietly resets it for everybody; give
 * the renamed property a `@SerialName` of the old name instead.
 */
@Serializable
data class UserPreferences(
    val category: CategoryPreference = CategoryPreference(),
    val media: MediaPreference = MediaPreference(),
    val notification: NotificationPreference = NotificationPreference(),
    val people: PeoplePreference = PeoplePreference(),
    val place: PlacePreference = PlacePreference(),
    val random: RandomPreference = RandomPreference(),
    val search: SearchPreference = SearchPreference(),
)

// the store emits whenever any setting changes, so each reader is narrowed to the part it asked
// about and only hears of changes to that
fun <T> DataStore<UserPreferences>.select(selector: (UserPreferences) -> T): Flow<T> =
    data.map(selector).distinctUntilChanged()
