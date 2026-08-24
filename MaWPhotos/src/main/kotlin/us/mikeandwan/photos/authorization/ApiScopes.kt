package us.mikeandwan.photos.authorization

/**
 * The scopes the API defines, mirroring ApiScopes.cs in maw-media.
 *
 * These are the unqualified names. Auth0 issues scopes for a custom API prefixed with the API
 * identifier (audience), and the API requires them in that qualified form, so both the login
 * request and the check against a granted token qualify them first - see [AuthService.getScopes]
 * and [AuthService.qualify].
 */
object ApiScopes {
    const val MEDIA_READ = "media:read"
    const val MEDIA_WRITE = "media:write"
    const val COMMENTS_READ = "comments:read"
    const val COMMENTS_WRITE = "comments:write"

    // spans /persons, /clans, /media/{id}/faces and the face crops served under /assets/faces,
    // so the whole face feature can be withdrawn from a client on its own
    const val FACE_RECOGNITION_READ = "face-recognition:read"
}
