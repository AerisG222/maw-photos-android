package us.mikeandwan.photos.authorization

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccessTokenScopesTest {
    @Test
    fun `reads space delimited scopes from the scope claim`() {
        val token = tokenWith("""{"scope":"openid $MEDIA_READ $FACE_READ"}""")

        assertEquals(setOf("openid", MEDIA_READ, FACE_READ), decodeGrantedScopes(token))
    }

    @Test
    fun `reads the scp claim, which the api accepts in place of scope`() {
        val token = tokenWith("""{"scp":"$MEDIA_READ"}""")

        assertEquals(setOf(MEDIA_READ), decodeGrantedScopes(token))
    }

    @Test
    fun `reads scopes issued as an array`() {
        val token = tokenWith("""{"scp":["$MEDIA_READ","$FACE_READ"]}""")

        assertEquals(setOf(MEDIA_READ, FACE_READ), decodeGrantedScopes(token))
    }

    @Test
    fun `a token granting a narrower set does not report the missing scope`() {
        val token = tokenWith("""{"scope":"openid $MEDIA_READ"}""")

        assertEquals(setOf("openid", MEDIA_READ), decodeGrantedScopes(token))
    }

    // the cases below all mean "cannot tell", which callers must not read as "grants nothing"

    @Test
    fun `an opaque token reports unknown rather than an empty grant`() {
        assertNull(decodeGrantedScopes("2YotnFZFEjr1zCsicMWpAA"))
    }

    @Test
    fun `a token with no scope claim reports unknown`() {
        assertNull(decodeGrantedScopes(tokenWith("""{"sub":"auth0|1"}""")))
    }

    @Test
    fun `an undecodable payload reports unknown`() {
        assertNull(decodeGrantedScopes("header.'not base64'.signature"))
    }

    @Test
    fun `a payload that is not an object reports unknown`() {
        assertNull(decodeGrantedScopes(tokenWith("[]")))
    }

    // base64url without padding, as a real token carries it
    private fun tokenWith(payload: String): String {
        val encoded = Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray(Charsets.UTF_8))

        return "header.$encoded.signature"
    }

    companion object {
        private const val AUDIENCE = "https://dev-media.mikeandwan.us"
        private const val MEDIA_READ = "$AUDIENCE/media:read"
        private const val FACE_READ = "$AUDIENCE/face-recognition:read"
    }
}
