package us.mikeandwan.photos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import us.mikeandwan.photos.authorization.AuthService
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.FileStorageRepository
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.MediaRepository
import us.mikeandwan.photos.domain.guards.AuthGuard
import us.mikeandwan.photos.domain.guards.CategoriesLoadedGuard
import us.mikeandwan.photos.domain.services.MediaCommentService
import us.mikeandwan.photos.domain.services.MediaExifService
import us.mikeandwan.photos.domain.services.MediaListService
import us.mikeandwan.photos.domain.services.MediaFavoriteService

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
    fun providesMediaExifService(mediaRepository: MediaRepository): MediaExifService =
        MediaExifService(mediaRepository)

    @Provides
    @ViewModelScoped
    fun provideMediaListService(
        categoryRepository: CategoryRepository,
        fileRepository: FileStorageRepository,
        mediaFavoriteService: MediaFavoriteService,
        mediaCommentService: MediaCommentService,
        mediaExifService: MediaExifService
    ): MediaListService =
        MediaListService(
            categoryRepository,
            fileRepository,
            mediaFavoriteService,
            mediaCommentService,
            mediaExifService
        )

    @Provides
    @ViewModelScoped
    fun provideAuthGuard(authService: AuthService): AuthGuard =
        AuthGuard(authService)

    @Provides
    @ViewModelScoped
    fun provideCategoriesLoadedGuard(
        categoryRepository: CategoryRepository,
        errorRepository: ErrorRepository
    ): CategoriesLoadedGuard =
        CategoriesLoadedGuard(
            categoryRepository,
            errorRepository
        )
}
