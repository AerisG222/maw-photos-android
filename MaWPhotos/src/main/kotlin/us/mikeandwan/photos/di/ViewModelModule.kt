package us.mikeandwan.photos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.FaceFeedRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.MediaFaceRepository
import us.mikeandwan.photos.domain.MediaPreferenceRepository
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.RandomMediaRepository
import us.mikeandwan.photos.domain.services.MediaCommentService
import us.mikeandwan.photos.domain.services.MediaExifService
import us.mikeandwan.photos.domain.services.MediaFaceService
import us.mikeandwan.photos.domain.services.MediaFavoriteService
import us.mikeandwan.photos.domain.services.MediaListService

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    @Provides
    @ViewModelScoped
    fun providesMediaRatingService(mediaRepository: MediaRepository): MediaFavoriteService =
        MediaFavoriteService(mediaRepository)

    @Provides
    @ViewModelScoped
    fun providesMediaCommentService(mediaRepository: MediaRepository): MediaCommentService =
        MediaCommentService(mediaRepository)

    @Provides
    @ViewModelScoped
    fun providesMediaExifService(mediaRepository: MediaRepository): MediaExifService = MediaExifService(mediaRepository)

    @Provides
    @ViewModelScoped
    fun providesMediaFaceService(
        mediaFaceRepository: MediaFaceRepository,
        peopleRepository: PeopleRepository,
    ): MediaFaceService = MediaFaceService(mediaFaceRepository, peopleRepository)

    @Provides
    @ViewModelScoped
    fun provideMediaListService(
        categoryRepository: CategoryRepository,
        randomMediaRepository: RandomMediaRepository,
        faceFeedRepository: FaceFeedRepository,
        fileRepository: FileStorageRepository,
        mediaFavoriteService: MediaFavoriteService,
        mediaCommentService: MediaCommentService,
        mediaExifService: MediaExifService,
        mediaFaceService: MediaFaceService,
        mediaPreferenceRepository: MediaPreferenceRepository,
        authService: AuthService,
    ): MediaListService =
        MediaListService(
            categoryRepository,
            randomMediaRepository,
            faceFeedRepository,
            fileRepository,
            mediaFavoriteService,
            mediaCommentService,
            mediaExifService,
            mediaFaceService,
            mediaPreferenceRepository,
            authService,
        )
}
