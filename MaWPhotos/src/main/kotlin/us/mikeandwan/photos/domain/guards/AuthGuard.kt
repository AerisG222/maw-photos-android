package us.mikeandwan.photos.domain.guards

import javax.inject.Inject
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.authorization.AuthStatus

class AuthGuard
    @Inject
    constructor(
        authService: AuthService,
    ) : IGuard {
        override val status = authService
            .authStatus
            .map {
                if (
                    it == AuthStatus.Authorized
                    // todo: we removed the prior check, is it still needed?
                    //  ||authorizationRepository.authState.value.refreshToken != null
                ) {
                    GuardStatus.Passed
                } else {
                    GuardStatus.Failed
                }
            }

        override fun initializeGuard() {
            // no additional initialization
        }
    }
