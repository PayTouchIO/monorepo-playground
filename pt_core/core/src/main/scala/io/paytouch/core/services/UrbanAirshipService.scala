package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes

import cats.data._
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.clients.urbanairship._
import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.entities.{ Address, GiftCard, LoyaltyProgram, MerchantContext }
import io.paytouch.core.entities.enums.PassType
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.core.json.JsonSupport.JValue
import io.paytouch.core.ServiceConfigurations

class UrbanAirshipService(
    urbanAirshipProjectIds: ProjectIds,
    locationService: LocationService,
    merchantService: MerchantService,
    walletClient: WalletClient,
  )(implicit
    val ec: ExecutionContext,
  ) extends LazyLogging {

  def upsertPass[A](
      templateId: String,
      passType: PassType,
      passData: A,
    )(implicit
      passLoader: PassLoader[A],
    ): Future[PassResponse] = {
    val passId = passLoader.passIdByPassType(passType, passData)
    val passUpsertion = passLoader.upsertionData(passData)
    val result = walletClient.updatePass(passId, passUpsertion).flatMap { response =>
      recoverIfNotFoundOrFail(response, createPass(templateId, passId, passUpsertion))
    }
    result.failed.foreach { exception =>
      val error =
        s"[urbanairship] Something went wrong while converting body: ${exception}. exception_trace=${Logging
          .stackTraceFor(exception)} current_trace=${Logging.stackTraceFor(new Throwable())}"
      logger.error(error)
    }
    result
  }

  private def createPass[A](
      templateId: String,
      passId: String,
      passUpsertion: PassUpsertion,
    ): Future[PassResponse] =
    walletClient.createPass(templateId, passId, passUpsertion).flatMap(getOrFail)

  def upsertTemplate[A <: TemplateData](
      localItemId: UUID,
      passType: PassType,
      templateData: A,
    )(implicit
      configLoader: ConfigLoader[A],
      loader: TemplateLoader[A],
    ): Future[TemplateUpserted] = {
    val projectId = configLoader.extractProjectId(urbanAirshipProjectIds)
    val externalId = s"${passType.entryName}-${localItemId.toString}"
    val template = loader.loadTemplate(passType, templateData)

    walletClient.updateTemplateWithExternalId(externalId, template).flatMap { response =>
      recoverIfNotFoundOrFail(response, createTemplate(projectId, externalId, template))
    }
  }

  private def createTemplate[A <: TemplateData](
      projectId: String,
      externalId: String,
      template: JValue,
    ) =
    walletClient
      .createTemplateWithProjectIdAndExternalId(projectId, externalId, template)
      .flatMap(getOrFail)

  def prepareLoyaltyTemplateData(
      merchantId: UUID,
      loyaltyProgram: LoyaltyProgram,
    ): Future[Option[TemplateData.LoyaltyTemplateData]] =
    (for {
      merchant <- OptionT(merchantService.findById(merchantId)(MerchantExpansions.none))
      merchantContext = MerchantContext.extract(merchant)
      firstLocation <- OptionT(locationService.findFirstLocation(merchantContext))
    } yield {
      val iconImage =
        loyaltyProgram
          .iconImageUrls
          .headOption
          .flatMap(_.urls.get("small"))

      TemplateData.LoyaltyTemplateData(
        merchantName = loyaltyProgram.businessName,
        iconImage = iconImage,
        logoImage = iconImage,
        address = Some(prepareAddress(firstLocation.address)),
        details = loyaltyProgram.templateDetails,
        phone = Some(firstLocation.phoneNumber),
        website = firstLocation.website.getOrElse(ServiceConfigurations.defaultMerchantWebsite),
      )
    }).value

  def prepareGiftCardTemplateData(
      merchantId: UUID,
      giftCard: GiftCard,
    ): Future[Option[TemplateData.GiftCardTemplateData]] =
    (for {
      merchant <- OptionT(merchantService.findById(merchantId)(MerchantExpansions.none))
      merchantContext = MerchantContext.extract(merchant)
      firstLocation <- OptionT(locationService.findFirstLocation(merchantContext))
    } yield {
      val iconImage =
        giftCard
          .avatarImageUrls
          .headOption
          .flatMap(_.urls.get("small"))

      TemplateData.GiftCardTemplateData(
        merchantName = giftCard.businessName,
        currentBalance = 0.USD,
        originalBalance = 0.USD,
        lastSpend = None,
        address = Some(prepareAddress(firstLocation.address)),
        details = giftCard.templateDetails,
        androidImage = iconImage,
        appleFullWidthImage = None,
        logoImage = iconImage,
        phone = Some(firstLocation.phoneNumber),
        website = firstLocation.website.getOrElse(ServiceConfigurations.defaultMerchantWebsite),
      )
    }).value

  private def prepareAddress(address: Address) = {
    val piece1 = Seq(address.line1, address.line2).flatten.mkString(" ")
    val piece2 = Seq(address.postalCode, address.city, address.state).flatten.mkString(" ")
    s"$piece1\n$piece2"
  }

  private def recoverIfNotFoundOrFail[T](response: UAResponse[T], recoveryF: => Future[T]): Future[T] =
    response match {
      case Left(err) if err.status == StatusCodes.NotFound => recoveryF
      case result                                          => getOrFail(result)
    }

  private def getOrFail[T](response: UAResponse[T]): Future[T] =
    response match {
      case Right(result) => Future.successful(result)
      case Left(err) =>
        val msg = err.message
        logger.error(msg)
        Future.failed(new RuntimeException(msg))
    }
}
