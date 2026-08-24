package us.mikeandwan.photos.authorization

/**
 * Whether the credentials in hand carry a given scope.
 *
 * [Unknown] is a real and common state rather than a loading one: nothing has been read yet, the
 * user is not logged in, or the token could not be parsed. A caller gating a feature on this should
 * treat it as "do not nag" rather than as a denial - only [Denied] is a statement that the API will
 * refuse the calls behind the feature.
 */
enum class ScopeAccess {
    Unknown,
    Granted,
    Denied,
}
