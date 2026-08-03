package com.wishtrace.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
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
import com.wishtrace.app.data.importChosenContact
import com.wishtrace.app.ui.components.ShellDestination
import com.wishtrace.app.ui.components.WishTraceBottomBar
import com.wishtrace.app.ui.screens.auth.SignInRoute
import com.wishtrace.app.ui.screens.checkout.CheckoutScreen
import com.wishtrace.app.ui.screens.discovery.GiftDiscoveryScreen
import com.wishtrace.app.ui.screens.giftdna.GiftDnaRoute
import com.wishtrace.app.ui.screens.giftdna.GiftDnaViewModel
import com.wishtrace.app.ui.screens.home.HomeScreen
import com.wishtrace.app.ui.screens.mandate.MandateSetupRoute
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
import com.wishtrace.app.ui.theme.BrandIndigo
import com.wishtrace.app.ui.theme.Canvas
import java.time.LocalDate
import kotlinx.coroutines.launch

private object Destination {
    const val Welcome = "welcome"
    const val SignIn = "sign_in"
    const val Recipient = "recipient"
    const val Discovery = "discovery"
    const val Recommendation = "recommendation"
    const val Checkout = "checkout"
    const val AddPerson = "add_person"
    const val EditPerson = "edit_person"
    const val EditOccasion = "edit_occasion"
    const val GiftDna = "gift_dna"
    const val MandateSetup = "mandate_setup"
}

private val UUID_REGEX =
    Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")

@Composable
fun WishTraceApp(
    pravaReturnUri: String? = null,
    onPravaReturnConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val container = remember(context.applicationContext) {
        AppContainer(context.applicationContext)
    }
    val homeFactory = remember(container) {
        viewModelFactory {
            HomeViewModel(
                repository = container.wishTraceRepository,
                mandateGateway = container.mandateGateway,
            )
        }
    }
    val discoveryFactory = remember(container) {
        viewModelFactory { DiscoveryViewModel(container.discoveryGateway) }
    }
    val peopleFactory = remember(container) {
        viewModelFactory { PeopleViewModel(container.peopleRepository) }
    }
    val homeViewModel: HomeViewModel = viewModel(factory = homeFactory)
    val discoveryViewModel: DiscoveryViewModel = viewModel(factory = discoveryFactory)
    val peopleViewModel: PeopleViewModel = viewModel(factory = peopleFactory)
    val checkoutFactory = remember(container) {
        viewModelFactory { CheckoutViewModel(container.purchaseFlowGateway) }
    }
    val checkoutViewModel: CheckoutViewModel = viewModel(factory = checkoutFactory)
    val mandateFactory = remember(container) {
        viewModelFactory { MandateSetupViewModel(container.mandateGateway) }
    }
    val mandateViewModel: MandateSetupViewModel = viewModel(factory = mandateFactory)

    val navController = rememberNavController()
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val peopleState by peopleViewModel.state.collectAsStateWithLifecycle()
    val discoveryState by discoveryViewModel.state.collectAsStateWithLifecycle()
    val checkoutState by checkoutViewModel.state.collectAsStateWithLifecycle()
    val mandateState by mandateViewModel.state.collectAsStateWithLifecycle()
    val session by container.authRepository.session.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val initialDestination = remember(container) {
        if (container.authRepository.session.value == null) {
            Destination.Welcome
        } else {
            ShellDestination.Home.route
        }
    }
    val shellRoutes = remember { ShellDestination.entries.map { it.route }.toSet() }
    val publicRoutes = remember { setOf(Destination.Welcome, Destination.SignIn) }
    val showShell = currentRoute in shellRoutes
    val scope = rememberCoroutineScope()
    val googleWebClientId = stringResource(R.string.google_web_client_id)
    var selectedCandidateId by rememberSaveable { mutableStateOf<String?>(null) }
    var photoRecipientId by rememberSaveable { mutableStateOf<String?>(null) }
    val existingContactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { contactUri ->
        val recipientId = photoRecipientId
        photoRecipientId = null
        if (contactUri != null && recipientId != null) {
            scope.launch {
                val imported = importChosenContact(context, contactUri)
                container.recipientPhotoStore.remember(recipientId, imported?.localPhotoUri)
                homeViewModel.retry()
                peopleViewModel.retry()
            }
        }
    }

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

    fun startFreshDiscovery() {
        discoveryViewModel.reset()
        selectedCandidateId = null
        navController.navigate(Destination.Discovery) {
            launchSingleTop = true
        }
    }

    fun openGiftJourney() {
        val content = homeState as? HomeUiState.Content
        val existingMandate = content?.mandate
        if (content != null && existingMandate != null) {
            mandateViewModel.openExisting(content.snapshot.occasion.id)
            navController.navigate(Destination.MandateSetup) {
                launchSingleTop = true
            }
        } else {
            startFreshDiscovery()
        }
    }

    LaunchedEffect(session, currentRoute) {
        when {
            session != null && currentRoute == Destination.Welcome -> enterApp()
            session == null && currentRoute != null && currentRoute !in publicRoutes -> {
                navController.navigate(Destination.Welcome) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(session?.accessToken) {
        if (session != null) {
            homeViewModel.retry()
            peopleViewModel.retry()
        }
    }

    LaunchedEffect(pravaReturnUri, session) {
        val uri = pravaReturnUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (uri == null) return@LaunchedEffect
        val purchaseIntentId = uri.getQueryParameter("purchase_intent_id")
        val occasionId = uri.getQueryParameter("occasion_id")
        val valid = uri.scheme == "wishtrace" &&
            uri.host == "prava" &&
            uri.path == "/return"
        if (!valid) {
            onPravaReturnConsumed()
            return@LaunchedEffect
        }
        val purchaseIntentValid = purchaseIntentId?.matches(UUID_REGEX) == true
        val occasionValid = occasionId?.matches(UUID_REGEX) == true
        if (!purchaseIntentValid && !occasionValid) {
            onPravaReturnConsumed()
            return@LaunchedEffect
        }
        if (session == null) return@LaunchedEffect
        if (purchaseIntentValid) {
            navController.navigate(Destination.Checkout) { launchSingleTop = true }
            checkoutViewModel.resumeFromReturn(requireNotNull(purchaseIntentId))
        }
        if (occasionValid) {
            navController.navigate(Destination.MandateSetup) { launchSingleTop = true }
            mandateViewModel.resumeFromReturn(requireNotNull(occasionId))
        }
        onPravaReturnConsumed()
    }

    LaunchedEffect(checkoutState.approvalUrl) {
        val hostedUrl = checkoutState.approvalUrl ?: return@LaunchedEffect
        val uri = runCatching { Uri.parse(hostedUrl) }.getOrNull()
        val safe = uri?.scheme == "https" &&
            uri.host in setOf("sandbox.collect.prava.space", "collect.prava.space") &&
            uri.userInfo == null
        if (!safe) {
            checkoutViewModel.approvalLaunchFailed()
            return@LaunchedEffect
        }
        checkoutViewModel.consumeApprovalUrl()
        runCatching {
            buildPravaCustomTab()
                .launchUrl(context, requireNotNull(uri))
        }.onFailure {
            checkoutViewModel.approvalLaunchFailed()
        }
    }

    LaunchedEffect(mandateState.approvalUrl) {
        val hostedUrl = mandateState.approvalUrl ?: return@LaunchedEffect
        val uri = runCatching { Uri.parse(hostedUrl) }.getOrNull()
        val safe = uri?.scheme == "https" &&
            uri.host in setOf("sandbox.collect.prava.space", "collect.prava.space") &&
            uri.userInfo == null
        if (!safe) {
            mandateViewModel.approvalLaunchFailed()
            return@LaunchedEffect
        }
        mandateViewModel.consumeApprovalUrl()
        runCatching {
            buildPravaCustomTab()
                .launchUrl(context, requireNotNull(uri))
        }.onFailure {
            mandateViewModel.approvalLaunchFailed()
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
                    onAdd = { navController.navigate(Destination.GiftDna) },
                )
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = initialDestination,
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
                        enterApp()
                    },
                    onBack = navController::popBackStack,
                )
            }
            composable(ShellDestination.Home.route) {
                HomeScreen(
                    state = homeState,
                    giverDisplayName = session?.user?.displayName,
                    onRetry = homeViewModel::retry,
                    onFindGift = ::openGiftJourney,
                    onFindAnotherGift = ::startFreshDiscovery,
                    onReviewRecipient = { navController.navigate(Destination.Recipient) },
                    onAddPerson = { navController.navigate(Destination.GiftDna) },
                )
            }
            composable(ShellDestination.People.route) {
                PeopleScreen(
                    state = peopleState,
                    onRetry = peopleViewModel::retry,
                    onAddPerson = { navController.navigate(Destination.GiftDna) },
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
                    onSignOut = {
                        scope.launch {
                            container.authRepository.logout()
                            runCatching {
                                container.googleCredentialClient.clearCredentialState()
                            }
                            navController.navigate(Destination.Welcome) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable(Destination.Recipient) {
                RecipientProfileScreen(
                    state = homeState,
                    onBack = navController::popBackStack,
                    onRetry = homeViewModel::retry,
                    onFindGift = ::openGiftJourney,
                    onEdit = { navController.navigate(Destination.EditPerson) },
                    onChooseContact = {
                        val recipientId = (homeState as? HomeUiState.Content)
                            ?.snapshot
                            ?.recipient
                            ?.id
                        if (recipientId != null) {
                            photoRecipientId = recipientId
                            existingContactPicker.launch(null)
                        }
                    },
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
                    val preparation = (discoveryState as? DiscoveryUiState.ReadyForRanking)
                        ?.preparation
                    RecommendationScreen(
                        snapshot = snapshot,
                        state = preparation?.let {
                            RecommendationUiState.Content(
                                candidates = it.candidates,
                                decision = it.decision,
                            )
                        } ?: RecommendationUiState.Error(
                            "Run live discovery before choosing a gift.",
                        ),
                        onBack = navController::popBackStack,
                        onRetry = navController::popBackStack,
                        onSelect = { candidateId ->
                            selectedCandidateId = candidateId
                            mandateViewModel.prepareSelection(snapshot.occasion.id)
                            navController.navigate(Destination.MandateSetup)
                        },
                        onWriteMessage = navController::popBackStack,
                    )
                }
            }
            composable(Destination.Checkout) {
                selectedCandidateId?.let { candidateId ->
                    LaunchedEffect(candidateId) { checkoutViewModel.start(candidateId) }
                }
                CheckoutScreen(
                    state = checkoutState,
                    verifiedEmail = session?.user?.email,
                    onBack = navController::popBackStack,
                    onBillingChange = { form ->
                        checkoutViewModel.updateBilling { form }
                    },
                    onUseSandboxBilling = checkoutViewModel::useSandboxBillingAddress,
                    onQuote = {
                        checkoutViewModel.requestQuote(session?.user?.email)
                    },
                    onApprove = checkoutViewModel::createApprovalSession,
                    onRefresh = checkoutViewModel::refresh,
                    onMessageChange = checkoutViewModel::updateMessage,
                    onSaveMessage = checkoutViewModel::saveMessage,
                )
            }
            composable(Destination.GiftDna) {
                val today = (homeState as? HomeUiState.Content)
                    ?.snapshot
                    ?.today
                    ?: LocalDate.now()
                val giftDnaFactory = remember(container, today) {
                    viewModelFactory {
                        GiftDnaViewModel(
                            peopleRepository = container.peopleRepository,
                            occasionRepository = container.occasionRepository,
                            today = today,
                            recipientPhotoStore = container.recipientPhotoStore,
                        )
                    }
                }
                val giftDnaViewModel: GiftDnaViewModel = viewModel(
                    key = "gift-dna",
                    factory = giftDnaFactory,
                )
                GiftDnaRoute(
                    viewModel = giftDnaViewModel,
                    onBack = navController::popBackStack,
                    onSaved = {
                        navController.popBackStack()
                        homeViewModel.retry()
                        peopleViewModel.retry()
                    },
                )
            }
            composable(Destination.MandateSetup) {
                val snapshotOccasionId = remember {
                    (homeState as? HomeUiState.Content)?.snapshot?.occasion?.id
                }
                // The occasion id can arrive two ways: from the home snapshot (hero path
                // through Recommendation) or straight from the mandate VM after a deep-link
                // return reconciled it. The deep-link cold start has no candidate id, but by
                // then the user is past arming, so an empty candidate is never exercised.
                val occasionId = mandateState.occasionId ?: snapshotOccasionId
                if (occasionId == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    MandateSetupRoute(
                        viewModel = mandateViewModel,
                        occasionId = occasionId,
                        candidateId = selectedCandidateId.orEmpty(),
                        verifiedEmail = session?.user?.email,
                        onBack = navController::popBackStack,
                        onChooseAnotherGift = {
                            discoveryViewModel.reset()
                            selectedCandidateId = null
                            navController.navigate(Destination.Discovery) {
                                popUpTo(Destination.MandateSetup) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onArmed = {
                            navController.popBackStack()
                            homeViewModel.retry()
                        },
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
                        peopleViewModel.retry()
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
                            peopleViewModel.retry()
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

private fun buildPravaCustomTab(): CustomTabsIntent {
    val colors = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(BrandIndigo.toArgb())
        .setNavigationBarColor(Canvas.toArgb())
        .build()
    return CustomTabsIntent.Builder()
        .setShowTitle(false)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .setDefaultColorSchemeParams(colors)
        .build()
}
