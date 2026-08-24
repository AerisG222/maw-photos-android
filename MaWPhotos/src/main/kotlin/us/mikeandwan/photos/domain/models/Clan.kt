package us.mikeandwan.photos.domain.models

import kotlin.uuid.Uuid

data class Clan(
    val id: Uuid,
    val name: String,
    // can be shorter than what was saved into the clan, or empty, when the caller can no longer see
    // some of its people
    val members: List<Person>,
)
