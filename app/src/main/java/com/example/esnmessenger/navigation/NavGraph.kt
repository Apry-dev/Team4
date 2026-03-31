package com.example.esnmessenger.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.esnmessenger.screens.ChatScreen
import com.example.esnmessenger.screens.HomeScreen
import com.example.esnmessenger.screens.LoginScreen
import com.example.esnmessenger.screens.OnboardingScreen
import com.example.esnmessenger.screens.RegisterScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val ONBOARDING = "onboarding?email={email}"
    const val HOME = "home"
    const val PROFILE_CHECK = "profile_check"
    const val CHAT = "chat/{otherUserId}"
    fun onboarding(email: String) = "onboarding?email=${Uri.encode(email)}"
    fun chat(otherUserId: String) = "chat/$otherUserId"
}

@Composable
private fun ProfileCheckScreen(onHasProfile: () -> Unit, onNoProfile: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(uid) {
        if (uid == null) { onNoProfile(); return@LaunchedEffect }
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.getString("email")?.isNotEmpty() == true) onHasProfile()
                else onNoProfile()
            }
            .addOnFailureListener { onHasProfile() }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun NavGraph(navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser != null) Routes.PROFILE_CHECK else Routes.LOGIN
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.PROFILE_CHECK) {
            ProfileCheckScreen(
                onHasProfile = {
                    navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                },
                onNoProfile = {
                    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    navController.navigate(Routes.onboarding(email)) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { registeredEmail ->
                    navController.navigate(Routes.onboarding(registeredEmail)) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.ONBOARDING,
            arguments = listOf(navArgument("email") { defaultValue = "" })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            OnboardingScreen(
                email = email,
                onOnboardingComplete = {
                    navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    GoogleSignIn.getClient(context, gso).signOut()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onOpenChat = { otherUserId ->
                    navController.navigate(Routes.chat(otherUserId))
                }
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("otherUserId") { defaultValue = "" })
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            ChatScreen(
                otherUserId = otherUserId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
