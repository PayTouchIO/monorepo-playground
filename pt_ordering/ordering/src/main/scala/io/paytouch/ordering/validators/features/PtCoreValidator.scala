package io.paytouch.ordering.validators.features

import java.util.UUID

import scala.concurrent._

import cats.data._

import io.paytouch.ordering.clients._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.entities.{ StoreContext, UserContext }
import io.paytouch.ordering.errors.{ FailedToFetchAllProducts, GiftCardPassByOnlineCodeNotFound }
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

trait PtCoreValidator {
  implicit def ec: ExecutionContext

  def ptCoreClient: PtCoreClient

  protected def validateCoreIds(coreIds: CoreIds)(implicit user: UserContext): Future[ValidatedData[Unit]] =
    ptCoreClient.idsValidate(coreIds)(user.authToken).map(_.asValidatedData)

  protected def validateProduct(productId: UUID)(implicit store: StoreContext): Future[ValidatedData[Product]] = {
    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore
    val expansions =
      ProductExpansions
        .empty
        .withModifiers
        .withStockLevel
        .withTaxRates
        .withVariants

    ptCoreClient
      .productsGet(productId, expansions)
      .map(_.asValidatedData)
  }

  def validateGiftCardPass(
      onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw,
    )(implicit
      store: StoreContext,
    ): Future[ValidatedData[GiftCardPass]] = {
    implicit val authHeader = ptCoreClient.generateAuthHeaderForCore

    ptCoreClient
      .giftCardPassesLookup(onlineCode)
      .map {
        case Right(response) => ValidatedData.success(response.data)
        case Left(_)         => ValidatedData.failure(GiftCardPassByOnlineCodeNotFound(onlineCode))
      }
  }

  protected def validateProducts(
      productIds: Seq[UUID],
    )(implicit
      store: StoreContext,
    ): Future[ValidatedData[Seq[Product]]] =
    if (productIds.isEmpty)
      Future.successful(ValidatedData.success(Seq.empty))
    else {
      implicit val authHeader = ptCoreClient.generateAuthHeaderForCore

      ptCoreClient.productsListByIds(productIds).map { result =>
        result.asValidatedData match {
          case Validated.Valid(products) if products.map(_.id).toSet != productIds.toSet =>
            ValidatedData.failure(FailedToFetchAllProducts(productIds, products.map(_.id)))

          case validOrInvalid => validOrInvalid
        }
      }
    }
}
