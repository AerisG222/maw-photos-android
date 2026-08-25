package us.mikeandwan.photos.domain.models

// mirrors the bounds ClanRoutes enforces in maw-media, so the UI can stop a request the API was
// always going to reject
object ClanRules {
    const val MAX_NAME_LENGTH = 100
    const val MAX_MEMBERS = 100
}
