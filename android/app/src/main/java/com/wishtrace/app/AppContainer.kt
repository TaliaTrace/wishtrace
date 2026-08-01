package com.wishtrace.app

import android.content.Context
import com.wishtrace.app.data.AuthRepository
import com.wishtrace.app.data.BackendPendingAuthRepository
import com.wishtrace.app.data.GiftDiscoveryGateway
import com.wishtrace.app.data.GoogleCredentialClient
import com.wishtrace.app.data.OccasionRepository
import com.wishtrace.app.data.PeopleRepository
import com.wishtrace.app.data.SeededGiftDiscoveryGateway
import com.wishtrace.app.data.SeededWishTraceRepository
import com.wishtrace.app.data.WishTraceRepository

class AppContainer(context: Context) {
    private val localRepository = SeededWishTraceRepository()
    val wishTraceRepository: WishTraceRepository = localRepository
    val peopleRepository: PeopleRepository = localRepository
    val occasionRepository: OccasionRepository = localRepository
    val discoveryGateway: GiftDiscoveryGateway = SeededGiftDiscoveryGateway()
    val authRepository: AuthRepository = BackendPendingAuthRepository()
    val googleCredentialClient = GoogleCredentialClient(context)
}
