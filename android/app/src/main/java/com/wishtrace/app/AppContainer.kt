package com.wishtrace.app

import android.content.Context
import com.wishtrace.app.data.AuthRepository
import com.wishtrace.app.data.BackendAuthRepository
import com.wishtrace.app.data.BackendWishTraceRepository
import com.wishtrace.app.data.GiftDiscoveryGateway
import com.wishtrace.app.data.GoogleCredentialClient
import com.wishtrace.app.data.OccasionRepository
import com.wishtrace.app.data.PeopleRepository
import com.wishtrace.app.data.SessionStore
import com.wishtrace.app.data.UnavailableGiftDiscoveryGateway
import com.wishtrace.app.data.WishTraceRepository
import com.wishtrace.app.data.WishTraceApiClient

class AppContainer(context: Context) {
    private val apiClient = WishTraceApiClient(context.getString(R.string.wishtrace_api_base_url))
    private val sessionStore = SessionStore(context)
    private val backendRepository = BackendWishTraceRepository(apiClient, sessionStore)
    val wishTraceRepository: WishTraceRepository = backendRepository
    val peopleRepository: PeopleRepository = backendRepository
    val occasionRepository: OccasionRepository = backendRepository
    val discoveryGateway: GiftDiscoveryGateway = UnavailableGiftDiscoveryGateway()
    val authRepository: AuthRepository = BackendAuthRepository(apiClient, sessionStore)
    val googleCredentialClient = GoogleCredentialClient(context)
}
