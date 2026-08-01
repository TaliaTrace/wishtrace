package com.wishtrace.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wishtrace.app.AppContainer
import com.wishtrace.app.R
import com.wishtrace.app.ui.components.ShellDestination
import com.wishtrace.app.ui.components.WishTraceBottomBar
import com.wishtrace.app.ui.screens.auth.SignInRoute
import com.wishtrace.app.ui.screens.discovery.GiftDiscoveryScreen
import com.wishtrace.app.ui.screens.home.HomeScreen
import com.wishtrace.app.ui.screens.message.MessageRoute
import com.wishtrace.app.ui.screens.message.MessageViewModel
import com.wishtrace.app.ui.screens.occasions.OccasionsScreen
import com.wishtrace.app.ui.screens.onboarding.WelcomeScreen
import com.wishtrace.app.ui.screens.people.PeopleScreen
import com.wishtrace.app.ui.screens.profile.ProfileScreen
import com.wishtrace.app.ui.screens.recipient.RecipientProfileScreen
import com.wishtrace.app.ui.screens.recommendation.RecommendationScreen
import com.wishtrace.app.ui.screens.recommendation.RecommendationUiState
import com.wishtrace.app.ui.screens.setup.RecipientSetupRoute
import com.wishtrace.app.ui.screens.setup.RecipientSetupStep
import com.wishtrace.app.ui.screens.setup.RecipientSetupViewModel
import com.wishtrace.app.domain.MessageOrigin
import com.wishtrace.app.domain.PersonalMessage
import java.time.LocalDate
import kotlinx.coroutines.launch

private object Destination {
    const val Welcome = "welcome"
    const val SignIn = "sign_in"
    const val Recipient = "recipient"
    const val Discovery = "discovery"
    const val Recommendation = "recommendation"
    const val Message = "message"
    const val AddPerson = "add_person"
    const val EditPerson = "edit_person"
    const val EditOccasion = "edit_occasion"
}

@Composable
fun WishTraceApp() {
    val context = LocalContext.current
    val container = remember(context.applicationContext) {
        AppContainer(context.applicationContext)
    }
    val homeFactory = remember(container) {
        viewModelFactory { HomeViewModel(container.wishTraceRepository) }
    }
    val discoveryFactory = remember(container) {
        viewModelFactory { DiscoveryViewModel(container.discoveryGateway) }
    }
    val homeViewModel: HomeViewModel = viewModel(factory = homeFactory)
    val discoveryViewModel: DiscoveryViewModel = viewModel(factory = discoveryFactory)
    val sessionViewModel: AppSessionViewModel = viewModel()

    val navController = rememberNavController()
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val discoveryState by discoveryViewModel.state.collectAsStateWithLifecycle()
    val session by sessionViewModel.session.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val shellRoutes = remember { ShellDestination.entries.map { it.route }.toSet() }
    val showShell = currentRoute in shellRoutes
    val scope = rememberCoroutineScope()
    val googleWebClientId = stringResource(R.string.google_web_client_id)
    var savedMessage by remember { mutableStateOf<PersonalMessage?>(null) }

    fun enterApp() {
        navController.navigate(ShellDestination.Home.route) {
            popUpTo(Destination.Welcome) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateTopLevel(destination: ShellDestination) {
        navController.navigate(destination.route) {
            popUpTo(ShellDestination.Home.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showShell) {
                WishTraceBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = ::navigateTopLevel,
                )
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Welcome,
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
            enterTransition = {
                fadeIn(tween(220)) + scaleIn(
                    animationSpec = tween(220),
                    initialScale = 0.99f,
                )
            },
            exitTransition = {
                fadeOut(tween(120)) + scaleOut(
                    animationSpec = tween(120),
                    targetScale = 0.995f,
                )
            },
            popEnterTransition = {
                fadeIn(tween(200)) + scaleIn(
                    animationSpec = tween(200),
                    initialScale = 0.995f,
                )
            },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(Destination.Welcome) {
                WelcomeScreen(
                    onGetStarted = { navController.navigate(Destination.SignIn) },
                )
            }
            composable(Destination.SignIn) {
                SignInRoute(
                    credentialClient = container.googleCredentialClient,
                    authRepository = container.authRepository,
                    webClientId = googleWebClientId,
                    onSignedIn = {
                        sessionViewModel.acceptVerifiedSession(it)
                        enterApp()
                    },
                    onContinueLocal = {
                        sessionViewModel.enterLocal()
                        enterApp()
                    },
                    onBack = navController::popBackStack,
                )
            }
            composable(ShellDestination.Home.route) {
                HomeScreen(
                    state = homeState,
                    onRetry = homeViewModel::retry,
                    onFindGift = { navController.navigate(Destination.Discovery) },
                    onReviewRecipient = { navController.navigate(Destination.Recipient) },
                    onAddPerson = { navController.navigate(Destination.AddPerson) },
                )
            }
            composable(ShellDestination.People.route) {
                PeopleScreen(
                    state = homeState,
                    onRetry = homeViewModel::retry,
                    onOpenRecipient = { navController.navigate(Destination.Recipient) },
                    onAddPerson = { navController.navigate(Destination.AddPerson) },
                    onEditPerson = { navController.navigate(Destination.EditPerson) },
                )
            }
            composable(ShellDestination.Occasions.route) {
                OccasionsScreen(
                    state = homeState,
                    onRetry = homeViewModel::retry,
                    onOpenOccasion = { navController.navigate(Destination.Recipient) },
                    onAddOccasion = { navController.navigate(Destination.AddPerson) },
                    onEditOccasion = { navController.navigate(Destination.EditOccasion) },
                )
            }
            composable(ShellDestination.Profile.route) {
                ProfileScreen(
                    session = session,
                    onResetLocal = {
                        sessionViewModel.enterLocal()
                        homeViewModel.resetLocalData()
                    },
                    onSignOut = {
                        scope.launch {
                            runCatching {
                                container.googleCredentialClient.clearCredentialState()
                            }
                        }
                        sessionViewModel.signOut()
                        navController.navigate(Destination.Welcome) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destination.Recipient) {
                RecipientProfileScreen(
                    state = homeState,
                    onBack = navController::popBackStack,
                    onRetry = homeViewModel::retry,
                    onFindGift = { navController.navigate(Destination.Discovery) },
                    onEdit = { navController.navigate(Destination.EditPerson) },
                )
            }
            composable(Destination.Discovery) {
                GiftDiscoveryScreen(
                    homeState = homeState,
                    state = discoveryState,
                    onBack = {
                        discoveryViewModel.cancel()
                        navController.popBackStack()
                    },
                    onStart = discoveryViewModel::start,
                    onCancel = discoveryViewModel::cancel,
                    onContinue = {
                        navController.navigate(Destination.Recommendation)
                    },
                )
            }
            composable(Destination.Recommendation) {
                val snapshot = remember {
                    (homeState as? HomeUiState.Content)?.snapshot
                }
                if (snapshot == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    RecommendationScreen(
                        snapshot = snapshot,
                        state = RecommendationUiState.SourceNeeded,
                        onBack = navController::popBackStack,
                        onRetry = navController::popBackStack,
                        onSelect = {
                            navController.navigate(Destination.Message)
                        },
                        onWriteMessage = {
                            navController.navigate(Destination.Message)
                        },
                    )
                }
            }
            composable(Destination.Message) {
                val snapshot = remember {
                    (homeState as? HomeUiState.Content)?.snapshot
                }
                if (snapshot == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    val messageFactory = remember(snapshot.recipient.id, savedMessage) {
                        viewModelFactory {
                            MessageViewModel(
                                initialText = savedMessage?.text.orEmpty(),
                                initialOrigin = savedMessage?.origin ?: MessageOrigin.USER,
                            )
                        }
                    }
                    val messageViewModel: MessageViewModel = viewModel(
                        key = "message-${snapshot.recipient.id}",
                        factory = messageFactory,
                    )
                    MessageRoute(
                        viewModel = messageViewModel,
                        snapshot = snapshot,
                        onBack = navController::popBackStack,
                        onSaved = {
                            savedMessage = it
                            navController.popBackStack()
                        },
                        onSkip = navController::popBackStack,
                    )
                }
            }
            composable(Destination.AddPerson) {
                val today = (homeState as? HomeUiState.Content)
                    ?.snapshot
                    ?.today
                    ?: LocalDate.now()
                val setupFactory = remember(container, today) {
                    viewModelFactory {
                        RecipientSetupViewModel(
                            peopleRepository = container.peopleRepository,
                            occasionRepository = container.occasionRepository,
                            today = today,
                            initialSnapshot = null,
                        )
                    }
                }
                val setupViewModel: RecipientSetupViewModel = viewModel(
                    key = "add-person",
                    factory = setupFactory,
                )
                RecipientSetupRoute(
                    viewModel = setupViewModel,
                    title = "Add a person",
                    occasionOnly = false,
                    onBack = navController::popBackStack,
                    onSaved = {
                        navController.popBackStack()
                        homeViewModel.retry()
                    },
                )
            }
            composable(Destination.EditPerson) {
                val snapshot = remember {
                    (homeState as? HomeUiState.Content)?.snapshot
                }
                if (snapshot == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    val setupFactory = remember(container, snapshot) {
                        viewModelFactory {
                            RecipientSetupViewModel(
                                peopleRepository = container.peopleRepository,
                                occasionRepository = container.occasionRepository,
                                today = snapshot.today,
                                initialSnapshot = snapshot,
                            )
                        }
                    }
                    val setupViewModel: RecipientSetupViewModel = viewModel(
                        key = "edit-person-${snapshot.recipient.id}",
                        factory = setupFactory,
                    )
                    RecipientSetupRoute(
                        viewModel = setupViewModel,
                        title = "Edit details",
                        occasionOnly = false,
                        onBack = navController::popBackStack,
                        onSaved = {
                            navController.popBackStack()
                            homeViewModel.retry()
                        },
                    )
                }
            }
            composable(Destination.EditOccasion) {
                val snapshot = remember {
                    (homeState as? HomeUiState.Content)?.snapshot
                }
                if (snapshot == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    val setupFactory = remember(container, snapshot) {
                        viewModelFactory {
                            RecipientSetupViewModel(
                                peopleRepository = container.peopleRepository,
                                occasionRepository = container.occasionRepository,
                                today = snapshot.today,
                                initialSnapshot = snapshot,
                                initialStep = RecipientSetupStep.OCCASION,
                            )
                        }
                    }
                    val setupViewModel: RecipientSetupViewModel = viewModel(
                        key = "edit-occasion-${snapshot.occasion.id}",
                        factory = setupFactory,
                    )
                    RecipientSetupRoute(
                        viewModel = setupViewModel,
                        title = "Edit occasion",
                        occasionOnly = true,
                        onBack = navController::popBackStack,
                        onSaved = {
                            navController.popBackStack()
                            homeViewModel.retry()
                        },
                    )
                }
            }
        }
    }

}
