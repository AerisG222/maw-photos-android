package us.mikeandwan.photos.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object UserPreferencesSerializer : Serializer<UserPreferences> {
    private val json = Json {
        // a setting that has since been removed is dropped rather than failing the whole read, and
        // one whose stored value is no longer valid - an enum constant that has gone - falls back
        // to its default
        ignoreUnknownKeys = true
        coerceInputValues = true
        // written out in full so what is on disk is what the person saw, rather than silently
        // following a default that is changed in a later release
        encodeDefaults = true
    }

    override val defaultValue = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences =
        try {
            json.decodeFromString(input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to read preferences", e)
        }

    // datastore only ever calls this from its own scope, which runs on Dispatchers.IO - the write is
    // already where blocking belongs, and the inspection simply cannot see which dispatcher that is
    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(
        t: UserPreferences,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
