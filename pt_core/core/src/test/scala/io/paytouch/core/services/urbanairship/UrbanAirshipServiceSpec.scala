package io.paytouch.core.services.urbanairship

import java.util.UUID

import com.softwaremill.macwire._

import io.paytouch.core.clients.urbanairship.WalletClient
import io.paytouch.core.ServiceConfigurations
import io.paytouch.core.services._

abstract class UrbanAirshipServiceSpec extends ServiceDaoSpec {
  abstract class UrbanAirshipServiceSpecContext extends ServiceDaoSpecContext {
    val urbanAirshipProjectIds = ServiceConfigurations.urbanAirshipProjectIds
    val urbanAirshipHost = ServiceConfigurations.urbanAirshipHost
    val urbanAirshipUsername = ServiceConfigurations.urbanAirshipUsername
    val urbanAirshipApiKey = ServiceConfigurations.urbanAirshipApiKey

    val walletClientMock = mock[WalletClient]
    val service = wire[UrbanAirshipService]
    val randomUuid = UUID.randomUUID
  }
}
