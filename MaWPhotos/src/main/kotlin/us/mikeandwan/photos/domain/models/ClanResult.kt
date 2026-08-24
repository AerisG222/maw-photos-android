package us.mikeandwan.photos.domain.models

/**
 * What came of writing to a clan.
 *
 * The two named failures are the ones a dialog has to say something specific about; everything else
 * has already been reported the way the rest of the app reports a failed call, so [Failed] carries
 * nothing.
 */
sealed class ClanResult {
    data class Success(
        val clan: Clan,
    ) : ClanResult()

    // the caller already has a clan by that name (409)
    data object DuplicateName : ClanResult()

    // refused as invalid (400): a name past the limit, too many people, or somebody the caller
    // cannot see.  the first two are checked before sending, so in practice this is the third.
    data object Invalid : ClanResult()

    data object Failed : ClanResult()
}
