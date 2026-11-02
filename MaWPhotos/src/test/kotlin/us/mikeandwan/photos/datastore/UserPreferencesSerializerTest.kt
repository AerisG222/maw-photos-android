package us.mikeandwan.photos.datastore

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.MediaPreference
import us.mikeandwan.photos.domain.models.MediaType
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.PlacePreference

class UserPreferencesSerializerTest {
    @Test
    fun `saved filters survive being written and read back`() = runTest {
        val preferences = UserPreferences(
            media = MediaPreference(filters = MediaFilters(favoritesOnly = true)),
            people = PeoplePreference(
                filters = MediaFilters(favoritesOnly = true),
                feedFilters = MediaFilters(mediaType = MediaType.Video),
                feedShuffled = true,
            ),
            place = PlacePreference(
                feedFilters = MediaFilters(mediaType = MediaType.Photo),
                feedShuffled = true,
            ),
        )
        val output = ByteArrayOutputStream()

        UserPreferencesSerializer.writeTo(preferences, output)

        val read = UserPreferencesSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(preferences, read)
    }

    // what is on disk for anyone upgrading to the release that started saving filters
    @Test
    fun `a document from before filters were saved reads back unfiltered`() = runTest {
        val document = """{"search":{"recentQueryCountToSave":5},"people":{"showNames":false}}"""

        val read = UserPreferencesSerializer.readFrom(ByteArrayInputStream(document.toByteArray()))

        assertEquals(5, read.search.recentQueryCountToSave)
        assertEquals(false, read.people.showNames)
        assertEquals(MediaFilters(), read.search.filters)
        assertEquals(MediaFilters(), read.people.feedFilters)
        assertEquals(false, read.people.feedShuffled)
    }
}
