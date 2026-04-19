package com.example.esnmessenger.navigation

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.esnmessenger.R
import com.example.esnmessenger.ui.theme.ESNCyan
import com.example.esnmessenger.ui.theme.ESNCyanDark
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

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(700), label = "logoAlpha")
    val scale by animateFloatAsState(if (visible) 1f else 0.82f, tween(700), label = "logoScale")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "ESN Messenger Logo",
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "ESN Messenger",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
            Text(
                text = "Connect with students worldwide",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
            Spacer(Modifier.height(52.dp))
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
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
