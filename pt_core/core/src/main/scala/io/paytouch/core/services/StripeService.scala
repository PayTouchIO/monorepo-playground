package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch._
import io.paytouch.core.clients.stripe._
import io.paytouch.core.clients.stripe.entities._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.MerchantRecord
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators._

final class StripeService(
    stripeClient: StripeClient,
    stripeConnectClient: StripeConnectClient,
    merchantService: MerchantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LazyLogging {
  private val paymentTransactionValidator: PaymentTransactionValidator =
    new PaymentTransactionValidator(new OrderValidator)

  private val merchantValidator: MerchantValidator =
    new MerchantValidator

  def paymentTransactionRefund(
      paymentTransactionId: UUID,
      amount: Option[BigDecimal],
    )(implicit
      context: UserContext,
    ): Future[ErrorsOr[Refund]] =
    merchantService.getPaymentProcessorConfig[PaymentProcessorConfig.Stripe](PaymentProcessor.Stripe).flatMap {
      case Validated.Valid(merchantConfig) =>
        paymentTransactionValidator
          .validateRefund(paymentTransactionId, TransactionPaymentProcessor.Stripe, amount)
          .flatMap {
            case Validated.Valid(paymentTransaction) =>
              // TODO add to validator?
              val paymentIntentId = paymentTransaction.paymentDetails.get.gatewayTransactionReference.get
              stripeClient.refundPaymentIntent(merchantConfig, paymentIntentId, amount).map {
                case Left(error) =>
                  logger.warn(s"While refunding payment intent error = $error")
                  Multiple.failure(StripeClientError(error))

                case Right(refund) =>
                  Multiple.success(refund)
              }

            // Validation failure
            case i @ Validated.Invalid(_) =>
              i.pure[Future]
          }

      case i @ Validated.Invalid(_) =>
        i.pure[Future]
    }

  def connectCallback(
      connectRequest: StripeService.ConnectRequest,
    )(implicit
      context: UserContext,
    ): Future[ErrorsOr[Unit]] =
    merchantValidator
      .accessOneById(context.merchantId)
      .flatMap {
        case Validated.Valid(merchant) =>
          connectCallbackOrSucceedIfConfigIsAlreadyPresent(connectRequest, merchant)

        case invalid =>
          invalid.void.pure[Future]
      }

  private def connectCallbackOrSucceedIfConfigIsAlreadyPresent(
      connectRequest: StripeService.ConnectRequest,
      merchant: MerchantRecord,
    ): Future[ErrorsOr[Unit]] =
    merchant.paymentProcessor match {
      case PaymentProcessor.Stripe | PaymentProcessor.Paytouch =>
        merchant.paymentProcessorConfig match {
          case config: PaymentProcessorConfig.Stripe if config.nonEmpty =>
            Multiple
              .success(())
              .pure[Future]

          case _: PaymentProcessorConfig.Stripe | _: PaymentProcessorConfig.Paytouch =>
            stripeConnectClient.connectCallback(connectRequest).flatMap {
              case Right(response) =>
                merchantService
                  .updatePaymentProcessorConfig(
                    MerchantIdPostgres(merchant.id).cast,
                    response.toPaymentProcessorConfig,
                  )
                  .as(Multiple.success(()))

              case Left(error) =>
                Multiple
                  .failure(StripeClientError(error))
                  .pure[Future]
            }

          case otherConfig =>
            Multiple
              .failure(
                UnexpectedPaymentProcessorConfig(
                  actual = otherConfig.paymentProcessor,
                  expected = PaymentProcessor.Paytouch,
                  PaymentProcessor.Stripe,
                ),
              )
              .pure[Future]
        }

      case otherProcessor =>
        Multiple
          .failure(
            UnexpectedPaymentProcessor(
              actual = otherProcessor,
              expected = PaymentProcessor.Paytouch,
              PaymentProcessor.Stripe,
            ),
          )
          .pure[Future]
    }
}

object StripeService {
  final case class ConnectRequest(code: StripeService.Code)

  final case class Code(value: String) extends Opaque[String]
  case object Code extends OpaqueCompanion[String, Code]
  final case class Scope(value: String) extends Opaque[String]
  case object Scope extends OpaqueCompanion[String, Scope]

  final case class ConnectResponse(
      accessToken: Token.Access,
      refreshToken: Token.Refresh,
      liveMode: LiveMode,
      accountId: AccountId,
      publishableKey: PublishableKey,
    ) {
    def toPaymentProcessorConfig: PaymentProcessorConfig =
      PaymentProcessorConfig
        .Stripe(
          accessToken = accessToken.value,
          refreshToken = refreshToken.value,
          liveMode = liveMode.isLive,
          accountId = accountId.value,
          publishableKey = publishableKey.value,
        )
  }

  object Token {
    final case class Access(value: String) extends Opaque[String]
    case object Access extends OpaqueCompanion[String, Access]

    final case class Refresh(value: String) extends Opaque[String]
    case object Refresh extends OpaqueCompanion[String, Refresh]
  }

  sealed abstract class LiveMode {
    final def isLive: Boolean =
      this == LiveMode.Live

    final def isTest: Boolean =
      this == LiveMode.Test
  }

  object LiveMode {
    case object Live extends LiveMode
    case object Test extends LiveMode
  }

  final case class AccountId(value: String) extends Opaque[String]
  case object AccountId extends OpaqueCompanion[String, AccountId]

  final case class PublishableKey(value: String) extends Opaque[String]
  case object PublishableKey extends OpaqueCompanion[String, PublishableKey]
}
