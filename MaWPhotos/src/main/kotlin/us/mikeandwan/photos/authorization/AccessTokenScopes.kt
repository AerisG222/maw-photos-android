package us.mikeandwan.photos.authorization

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber

private val json = Json { ignoreUnknownKeys = true }

// the API reads either claim, treating both as space delimited - see ScopeAuthorizationHandler
// in maw-media.  scp is checked first for the same reason it is listed first there.
private val SCOPE_CLAIMS = listOf("scp", "scope")

/**
 * The scopes an access token actually carries.
 *
 * This is what the API enforces on, as opposed to what was asked for at login: a token minted
 * before a scope was added to the request will not carry it, and Auth0 will not add it to a token
 * refreshed from a grant that never included it.
 *
 * Returns null when the token says nothing about its scopes - it is opaque rather than a JWT, or
 * carries no scope claim. That is deliberately distinct from an empty set: "cannot tell" must not
 * be read as "holds nothing", or a token this cannot parse would look like a token that grants
 * nothing at all.
 */
fun decodeGrantedScopes(accessToken: String): Set<String>? {
    val segments = accessToken.split('.')

    if (segments.size < 2) {
        return null
    }

    return try {
        // base64url without padding, which the JDK decoder accepts as-is
        val payload = String(Base64.getUrlDecoder().decode(segments[1]), Charsets.UTF_8)
        val claims = json.parseToJsonElement(payload) as? JsonObject ?: return null

        SCOPE_CLAIMS
            .firstNotNullOfOrNull { claims[it] }
            ?.let { scopes ->
                when (scopes) {
                    // space delimited, as Auth0 issues it
                    is JsonPrimitive -> scopes.content.split(' ')

                    // some issuers hand back a list instead; both shapes cost one branch
                    is JsonArray -> scopes.mapNotNull { (it as? JsonPrimitive)?.content }

                    else -> null
                }
            }?.filter { it.isNotEmpty() }
            ?.toSet()
    } catch (t: Throwable) {
        Timber.w(t, "Unable to read the scopes granted by the access token")

        null
    }
}
