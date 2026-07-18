package com.example.gallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gallery.ui.screens.AlbumDetailScreen
import com.example.gallery.ui.screens.AlbumsScreen
import com.example.gallery.ui.screens.GalleryHomeScreen
import com.example.gallery.ui.screens.MediaViewerScreen
import com.example.gallery.viewmodel.GalleryViewModel

object Routes {
    const val HOME = "home"
    const val ALBUMS = "albums"
    const val ALBUM_DETAIL = "album/{bucketId}"
    const val VIEWER = "viewer/{source}/{bucketId}/{startIndex}"

    fun albumDetail(bucketId: String) = "album/$bucketId"
    fun viewer(source: String, bucketId: String, startIndex: Int) = "viewer/$source/$bucketId/$startIndex"
}

@Composable
fun GalleryNavGraph() {
    val navController = rememberNavController()
    val viewModel: GalleryViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            GalleryHomeScreen(
                viewModel = viewModel,
                onOpenAlbums = { navController.navigate(Routes.ALBUMS) },
                onOpenItem = { source, index ->
                    navController.navigate(Routes.viewer(source, "none", index))
                }
            )
        }

        composable(Routes.ALBUMS) {
            AlbumsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenAlbum = { bucketId -> navController.navigate(Routes.albumDetail(bucketId)) }
            )
        }

        composable(
            route = Routes.ALBUM_DETAIL,
            arguments = listOf(navArgument("bucketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getString("bucketId") ?: ""
            AlbumDetailScreen(
                viewModel = viewModel,
                bucketId = bucketId,
                onBack = { navController.popBackStack() },
                onOpenItem = { index ->
                    navController.navigate(Routes.viewer("album", bucketId, index))
                }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("startIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "all"
            val bucketId = backStackEntry.arguments?.getString("bucketId") ?: "none"
            val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0
            MediaViewerScreen(
                viewModel = viewModel,
                source = source,
                bucketId = bucketId,
                startIndex = startIndex,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
