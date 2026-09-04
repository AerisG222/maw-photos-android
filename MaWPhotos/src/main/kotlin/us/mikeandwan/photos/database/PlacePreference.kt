package us.mikeandwan.photos.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * How the places area draws what it lists.
 *
 * Its own table rather than columns on the people one: the two areas share a feed but are read
 * differently, and somebody who wants the year against every category while walking a country's
 * photographs is not thereby asking for it against a person's.
 */
@Entity(tableName = "place_preference")
data class PlacePreference(
    @PrimaryKey val id: Int,
    // what a category says about itself when a place's categories are being listed - see
    // CategoryLabels for why both start on
    @ColumnInfo(name = "show_category_year", defaultValue = "1") val showCategoryYear: Boolean,
    @ColumnInfo(name = "show_category_title", defaultValue = "1") val showCategoryTitle: Boolean,
)
