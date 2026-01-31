package us.mikeandwan.photos.domain.guards

sealed class GuardStatus {
    data object Passed : GuardStatus()

    data object Failed : GuardStatus()

    data object NotInitialized : GuardStatus()
}
