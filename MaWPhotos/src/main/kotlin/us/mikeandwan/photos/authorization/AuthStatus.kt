package us.mikeandwan.photos.authorization

sealed class AuthStatus {
    data object RequiresAuthorization : AuthStatus()

    data object LoginInProcess : AuthStatus()

    data object Authorized : AuthStatus()
}
