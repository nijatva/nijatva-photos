package com.vasudev.photos.ui.vm

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vasudev.photos.ZohoPhotosApplication

private fun CreationExtras.app(): ZohoPhotosApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZohoPhotosApplication

val PhotosVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { PhotosViewModel(app().container.repository) }
}

val AlbumsVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { AlbumsViewModel(app().container.repository) }
}

val AlbumDetailVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { AlbumDetailViewModel(createSavedStateHandle(), app().container.repository) }
}

val PhotoPagerVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { PhotoPagerViewModel(createSavedStateHandle(), app().container.repository) }
}

val SearchVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { SearchViewModel(app().container.repository) }
}

val VaultVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { VaultViewModel(app().container.repository) }
}

val TrashVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { TrashViewModel(app().container.repository) }
}

val CleanupVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { CleanupViewModel(app().container.repository) }
}

val SmartVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        SmartViewModel(
            app().container.smartIndexer,
            app().container.repository
        )
    }
}

val LensVMFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        LensViewModel(
            createSavedStateHandle(),
            app().container.repository,
            app()
        )
    }
}
