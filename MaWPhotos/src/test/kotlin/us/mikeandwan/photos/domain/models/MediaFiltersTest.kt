package us.mikeandwan.photos.domain.models

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFiltersTest {
    private val photo = media(MediaType.Photo, isFavorite = false)
    private val favoritePhoto = media(MediaType.Photo, isFavorite = true)
    private val favoriteVideo = media(MediaType.Video, isFavorite = true)
    private val allMedia = listOf(photo, favoritePhoto, favoriteVideo)

    @Test
    fun `counts each filter that is on`() {
        assertEquals(0, MediaFilters().activeCount)
        assertFalse(MediaFilters().isActive)
        assertEquals(1, MediaFilters(favoritesOnly = true).activeCount)
        assertEquals(2, MediaFilters(favoritesOnly = true, mediaType = MediaType.Video).activeCount)
        assertTrue(MediaFilters(mediaType = MediaType.Photo).isActive)
    }

    @Test
    fun `no filters leaves the media alone`() {
        assertEquals(allMedia, allMedia.filteredBy(MediaFilters()))
    }

    @Test
    fun `media must match every filter that is on`() {
        assertEquals(listOf(favoritePhoto, favoriteVideo), allMedia.filteredBy(MediaFilters(favoritesOnly = true)))
        assertEquals(listOf(photo, favoritePhoto), allMedia.filteredBy(MediaFilters(mediaType = MediaType.Photo)))
        assertEquals(
            listOf(favoritePhoto),
            allMedia.filteredBy(MediaFilters(favoritesOnly = true, mediaType = MediaType.Photo)),
        )
    }

    @Test
    fun `a category matches a type it holds any of, and favorites by its own flag`() {
        val mixed = category(isFavorite = false, MediaType.Photo, MediaType.Video)
        val favoritePhotos = category(isFavorite = true, MediaType.Photo)
        val categories = listOf(mixed, favoritePhotos)

        assertEquals(categories, categories.filteredCategoriesBy(MediaFilters()))
        assertEquals(listOf(mixed), categories.filteredCategoriesBy(MediaFilters(mediaType = MediaType.Video)))
        assertEquals(listOf(favoritePhotos), categories.filteredCategoriesBy(MediaFilters(favoritesOnly = true)))
        assertEquals(
            emptyList<Category>(),
            categories.filteredCategoriesBy(MediaFilters(favoritesOnly = true, mediaType = MediaType.Video)),
        )
    }

    @Test
    fun `types are spelled the way the api expects`() {
        assertEquals("photo", MediaType.Photo.toApiFilter())
        assertEquals("video", MediaType.Video.toApiFilter())
    }

    private fun media(
        type: MediaType,
        isFavorite: Boolean,
    ) = Media(id = Uuid.random(), categoryId = Uuid.random(), type = type, isFavorite = isFavorite)

    private fun category(
        isFavorite: Boolean,
        vararg mediaTypes: MediaType,
    ) = Category(
        id = Uuid.random(),
        year = 2024,
        name = "Category",
        effectiveDate = LocalDate(2024, 1, 1),
        modified = Instant.fromEpochMilliseconds(0),
        isFavorite = isFavorite,
        teaser = emptyList(),
        mediaTypes = mediaTypes.toList(),
    )
}
