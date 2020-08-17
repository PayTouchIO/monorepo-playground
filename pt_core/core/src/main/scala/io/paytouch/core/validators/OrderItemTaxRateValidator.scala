package io.paytouch.core.validators

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.TaxRateRecord
import io.paytouch.core.entities._
import io.paytouch.core.errors.{ EmptyTaxRateIds, InvalidTaxRateIds }

import io.paytouch.core.utils.Multiple._

import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import scala.concurrent._

class OrderItemTaxRateValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) {

  val taxRateValidator = new TaxRateValidator

  def validateUpsertions(
      taxRatesPerOrderId: Map[UUID, Seq[OrderItemTaxRateUpsertion]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[OrderItemTaxRateUpsertion]]] = {
    val taxRateIds = taxRatesPerOrderId.values.flatten.flatMap(_.taxRateId).toSeq
    taxRateValidator.filterValidByIds(taxRateIds).map { taxRates =>
      Multiple.sequence(
        taxRatesPerOrderId.map {
          case (orderItemId, upsertions) =>
            Multiple.combineSeq(
              upsertions.map { upsertion =>
                recoverTaxRateId(taxRates, upsertion.taxRateId) match {
                  case Valid(_)       => Multiple.success(upsertion)
                  case i @ Invalid(_) => i
                }
              },
            )
        },
      )
    }
  }

  def recoverUpsertions(
      taxRatesPerOrderId: Map[UUID, Seq[OrderItemTaxRateUpsertion]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[RecoveredOrderItemTaxRateUpsertion]]] = {
    val taxRateIds = taxRatesPerOrderId.values.flatten.flatMap(_.taxRateId).toSeq
    taxRateValidator.filterValidByIds(taxRateIds).map { taxRates =>
      taxRatesPerOrderId.map {
        case (orderItemId, upsertions) =>
          val recoveredUpsertions = upsertions.map { upsertion =>
            val recoveredTaxRateId = {
              val validatedTaxRateId = recoverTaxRateId(taxRates, upsertion.taxRateId)

              logger.loggedSoftRecover(validatedTaxRateId)("Tax rate not found, assuming it has been deleted")
            }

            RecoveredOrderItemTaxRateUpsertion(
              id = upsertion.id.getOrElse(UUID.randomUUID),
              taxRateId = recoveredTaxRateId,
              name = upsertion.name,
              value = upsertion.value,
              totalAmount = upsertion.totalAmount,
              applyToPrice = upsertion.applyToPrice,
              active = upsertion.active,
            )
          }

          orderItemId -> recoveredUpsertions
      }
    }
  }

  private def recoverTaxRateId(taxRates: Seq[TaxRateRecord], taxRateId: Option[UUID]): ErrorsOr[Option[UUID]] =
    taxRateId match {
      case Some(trId) if taxRates.exists(_.id == trId) => Multiple.successOpt(trId)
      case Some(trId)                                  => Multiple.failure(InvalidTaxRateIds(Seq(trId)))
      case None                                        => Multiple.failure(EmptyTaxRateIds(Seq.empty))
    }
}

final case class RecoveredOrderItemTaxRateUpsertion(
    id: UUID,
    taxRateId: Option[UUID],
    name: String,
    value: BigDecimal,
    totalAmount: Option[BigDecimal],
    applyToPrice: Boolean,
    active: Boolean,
  )
