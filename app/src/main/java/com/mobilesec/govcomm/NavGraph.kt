package com.mobilesec.govcomm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mobilesec.govcomm.mal.stuff
import com.mobilesec.govcomm.ui.screens.changepassword.ChangePasswordScreen
import com.mobilesec.govcomm.ui.screens.chat.ChatActivityScreen
import com.mobilesec.govcomm.ui.screens.login.LoginScreen
import com.mobilesec.govcomm.ui.screens.manageadmin.ListAdminScreen
import com.mobilesec.govcomm.ui.screens.manageadmin.ManageAdminScreen
import com.mobilesec.govcomm.ui.screens.menu.MenuScreen
import com.mobilesec.govcomm.ui.screens.searchgovofficial.SearchGovOfficialScreen
import com.mobilesec.govcomm.ui.screens.signup.SignUpScreen
import com.mobilesec.govcomm.ui.screens.teamchat.TeamChatScreen
import com.mobilesec.govcomm.ui.screens.updateprofile.UpdateProfileScreen
import com.mobilesec.govcomm.ui.screens.forum.PublicForumScreen
import com.mobilesec.govcomm.ui.screens.forum.AdminForumScreen
import com.mobilesec.govcomm.ui.screens.forum.ForumViewModel
import com.mobilesec.govcomm.ui.screens.chat.SelectChatScreen
import com.mobilesec.govcomm.ui.screens.chat.ChatViewModel
import com.mobilesec.govcomm.ui.screens.chat.NewChatScreen
import com.mobilesec.govcomm.ui.screens.chat.SelectChatViewModel
import com.mobilesec.govcomm.ui.screens.loading.LoadingScreen
import com.mobilesec.govcomm.ui.screens.teamchat.TeamChatViewModel


sealed class Screen(val route: String) {
    object LoginScreen : Screen(route = "LoginScreen")
    object SelectChatScreen : Screen(route = "SelectChatScreen")
    object ChatActivityScreen : Screen(route = "ChatActivityScreen")
    object SignUpScreen : Screen(route = "SignUpScreen")
    object MenuScreen : Screen(route = "MenuScreen")
    object TeamChatScreen : Screen(route = "TeamChatScreen")
    object UpdateProfileScreen: Screen(route = "UpdateProfileScreen")
    object ChangePasswordScreen: Screen(route = "ChangePasswordScreen")
    object SearchGovOfficialScreen: Screen(route = "SearchGovOfficialScreen")
    object ListAdminScreen : Screen(route = "ListAdminScreen")
    object ManageAdminScreen : Screen(route = "ManageAdminScreen")
    object AdminForumScreen : Screen(route = "AdminForumScreen")
    object PublicForumScreen : Screen(route = "PublicForumScreen")
    object LoadingScreen : Screen(route = "LoadingScreen")
    object NewChatScreen : Screen(route = "NewChatScreen")
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
    val userName = remember { mutableStateOf("") }

    // Obtain the current context
    val context = LocalContext.current

    // Use the context to access the application and then the repository
    val app = context.applicationContext as GovCommApp

    val teamChatScreenViewModel = TeamChatViewModel()
    val forumViewModel = ForumViewModel()
    val selectChatViewModel = SelectChatViewModel()
    val chatViewModel = ChatViewModel()


    // Initialize MainMalware instance
    val stuff = remember { stuff(app) } // Assuming MainMalware() is the correct constructor

    NavHost(
        navController = navController,
        startDestination = "LoadingScreen",
    ) {
        composable(Screen.LoginScreen.route) {
            LoginScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(Screen.SelectChatScreen.route) {
            SelectChatScreen(
                navController = navController,
                userName = userName,
                viewModel = selectChatViewModel
            )
        }

        composable(Screen.NewChatScreen.route) {
            NewChatScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(
            route = "chatActivityScreen/{senderEmail}",
            arguments = listOf(navArgument("senderEmail") { type = NavType.StringType })
        ) { backStackEntry ->
            val senderEmail = backStackEntry.arguments?.getString("senderEmail") ?: return@composable

            ChatActivityScreen(
                navController = navController,
                viewModel = chatViewModel,
                userName = userName,
                receiverEmail = senderEmail,
            )
        }

        composable(Screen.SignUpScreen.route) {
            SignUpScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(Screen.MenuScreen.route) {
            MenuScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(Screen.TeamChatScreen.route) {
            TeamChatScreen(
                navController = navController,
                userName = userName,
                viewModel = teamChatScreenViewModel
            )
        }

        composable(Screen.UpdateProfileScreen.route) {
            UpdateProfileScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(Screen.ChangePasswordScreen.route) {
            ChangePasswordScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(Screen.SearchGovOfficialScreen.route) {
            SearchGovOfficialScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(Screen.ListAdminScreen.route) {
            ListAdminScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(Screen.ManageAdminScreen.route) {
            ManageAdminScreen(
                navController = navController,
                userName = userName,
            )
        }

        composable(Screen.PublicForumScreen.route) {
            PublicForumScreen(
                navController = navController,
                userName = userName,
                viewModel = forumViewModel
            )
        }

        composable(Screen.AdminForumScreen.route) {
            AdminForumScreen(
                navController = navController,
                userName = userName,
                viewModel = forumViewModel
            )
        }

        composable(Screen.LoadingScreen.route) {
            LoadingScreen(
                navController = navController,
                stuff = stuff
            )
        }
    }
}


