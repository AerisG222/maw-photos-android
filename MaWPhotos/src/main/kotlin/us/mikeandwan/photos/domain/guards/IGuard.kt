package us.mikeandwan.photos.domain.guards

import kotlinx.coroutines.flow.Flow

interface IGuard {
    val status: Flow<GuardStatus>

    fun initializeGuard()
}
