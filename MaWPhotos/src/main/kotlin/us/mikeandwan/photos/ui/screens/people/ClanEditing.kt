package us.mikeandwan.photos.ui.screens.people

import us.mikeandwan.photos.domain.models.Clan

/**
 * What the face grid is currently being used for.
 *
 * Picking is a mode over the same grid rather than a separate screen, so the filter and sort that
 * make somebody findable are the same ones used to assemble a clan.
 */
sealed class ClanPicking {
    data object Off : ClanPicking()

    data object Create : ClanPicking()

    data class Members(
        val clan: Clan,
    ) : ClanPicking()
}

/**
 * Whether a clan is being named, and for which of the two reasons.
 *
 * Creating asks for the name last: it is easier to name a group once you can see who is in it.
 */
sealed class ClanNaming {
    data object Off : ClanNaming()

    data object Create : ClanNaming()

    data class Rename(
        val clan: Clan,
    ) : ClanNaming()
}
