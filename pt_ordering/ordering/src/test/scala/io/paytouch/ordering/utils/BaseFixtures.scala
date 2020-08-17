package io.paytouch.ordering.utils

import java.util.{ Currency, UUID }

import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials, OAuth2BearerToken }

import io.paytouch.ordering.clients.paytouch.core.entities.{ CoreUserContext, ImageUrls }
import io.paytouch.ordering.data.model.StoreRecord
import io.paytouch.ordering.entities.UserContext
import io.paytouch.ordering.stubs.PtCoreStubData
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory }

trait MultipleLocationFixtures extends BaseFixtures {
  lazy val londonDeliveryMinAmount: Option[BigDecimal] = None
  lazy val londonDeliveryMaxAmount: Option[BigDecimal] = None
  lazy val londonDeliveryFeeAmount: Option[BigDecimal] = None
  lazy val londonHeroImageUrl: Seq[ImageUrls] = Seq.empty
  lazy val londonLogoImageUrl: Seq[ImageUrls] = Seq.empty
  lazy val londonCatalogId: UUID = UUID.randomUUID
  lazy val londonId = UUID.randomUUID
  lazy val londonStore = Factory
    .store(
      merchant,
      locationId = londonId,
      catalogId = londonCatalogId,
      heroImageUrls = londonHeroImageUrl,
      logoImageUrls = londonLogoImageUrl,
      deliveryMinAmount = londonDeliveryMinAmount,
      deliveryMaxAmount = londonDeliveryMaxAmount,
      deliveryFeeAmount = londonDeliveryFeeAmount,
    )
    .create

  lazy val romeCatalogId: UUID = UUID.randomUUID
  lazy val romeId = UUID.randomUUID
  lazy val romeStore = Factory.store(merchant, locationId = romeId, catalogId = romeCatalogId).create

  lazy val stores = Seq(londonStore, romeStore)
}

trait DefaultFixtures extends BaseFixtures {
  lazy val londonCatalogId: UUID = UUID.randomUUID
  lazy val londonHeroImageUrls: Seq[ImageUrls] = Seq.empty
  lazy val londonLogoImageUrls: Seq[ImageUrls] = Seq.empty
  lazy val londonId = UUID.randomUUID
  lazy val londonStore = Factory
    .store(
      merchant,
      locationId = londonId,
      catalogId = londonCatalogId,
      heroImageUrls = londonHeroImageUrls,
      logoImageUrls = londonLogoImageUrls,
    )
    .create

  lazy val stores = Seq(londonStore)
}

trait BaseFixtures {
  lazy val merchant = Factory.merchant().create

  val competitor = Factory.merchant().create
  val competitorStore =
    Factory.store(competitor, catalogId = UUID.randomUUID, locationId = UUID.randomUUID, active = Some(true)).create

  def stores: Seq[StoreRecord]

  lazy val newYorkId = UUID.randomUUID
  lazy val sanFranciscoId = UUID.randomUUID
  lazy val torontoId = UUID.randomUUID
  lazy val parisId = UUID.randomUUID

  lazy val newYorkCatalogId = UUID.randomUUID
  lazy val sanFranciscoCatalogId = UUID.randomUUID
  lazy val torontoCatalogId = UUID.randomUUID
  lazy val parisCatalogId = UUID.randomUUID

  private lazy val extraLocationIds = Seq(newYorkId, sanFranciscoId, torontoId, parisId)

  val USD = Currency.getInstance("USD")

  lazy val merchantId = merchant.id
  lazy val userId = UUID.randomUUID
  private lazy val coreContext: CoreUserContext =
    CoreUserContext(
      id = userId,
      merchantId = merchantId,
      locationIds = stores.map(_.locationId) ++ extraLocationIds,
      currency = stores.headOption.map(_.currency).getOrElse(USD),
    )

  lazy val invalidAuthorizationHeader: Authorization =
    Authorization(OAuth2BearerToken("invalidToken"))

  lazy val userAuthorizationHeader: Authorization = {
    val token = Authorization(OAuth2BearerToken(userId.toString))
    PtCoreStubData.recordToken(token, coreContext)
    PtCoreStubData.recordLocationIds(coreContext.locationIds)(token)
    PtCoreStubData.recordCatalogIds(stores.map(_.catalogId))(token)
    token
  }

  implicit lazy val userContext: UserContext = UserContext(coreContext, userAuthorizationHeader)

  lazy val storeAuthorizationHeader = {
    import io.paytouch.ordering.ServiceConfigurations._
    Authorization(BasicHttpCredentials(storeUser, storePassword))
  }

  lazy val coreAuthorizationHeader = {
    import io.paytouch.ordering.ServiceConfigurations._
    Authorization(BasicHttpCredentials(coreUser, corePassword))
  }
}
