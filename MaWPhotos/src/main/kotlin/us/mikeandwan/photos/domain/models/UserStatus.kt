package us.mikeandwan.photos.domain.models

sealed class UserStatus {
    data object Unknown : UserStatus()
    data object Inactive : UserStatus()
    data class Active(val isAdmin: Boolean) : UserStatus()
}
