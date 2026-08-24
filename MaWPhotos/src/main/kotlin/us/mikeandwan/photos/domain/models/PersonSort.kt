package us.mikeandwan.photos.domain.models

/**
 * How the people grid is ordered.
 *
 * Favorites lead either ordering - marking somebody is how the handful of people actually looked
 * for get to the top, so it outranks whichever key is chosen here.
 */
enum class PersonSort {
    Name,
    MediaCount,
    ;

    fun next() = if (this == Name) MediaCount else Name
}
