package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.data.Validated.{ Invalid, Valid }

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._

class OrderTaxRateRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) {

  val taxRateValidator = new TaxRateValidator

  def validateUpsertions(
      upsertions: Seq[OrderTaxRateUpsertion],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Option[OrderTaxRateUpsertion]]]] = {
    val taxRateIds = upsertions.map(_.taxRateId)
    taxRateValidator.filterValidByIds(taxRateIds).map { taxRates =>
      Multiple.combineSeq(
        upsertions.map { upsertion =>
          recoverTaxRateId(taxRates, upsertion.taxRateId) match {
            case Valid(_)       => Multiple.successOpt(upsertion)
            case i @ Invalid(_) => i
          }
        },
      )
    }
  }

  def recoverUpsertions(
      upsertions: Seq[OrderTaxRateUpsertion],
    )(implicit
      user: UserContext,
    ): Future[Seq[RecoveredOrderTaxRateUpsertion]] = {
    val taxRateIds = upsertions.map(_.taxRateId)
    taxRateValidator.filterValidByIds(taxRateIds).map { taxRates =>
      upsertions.map { upsertion =>
        val recoveredTaxRateId = logger.loggedSoftRecover(recoverTaxRateId(taxRates, upsertion.taxRateId))(
          "Tax rate not found, assuming it has been deleted",
        )
        toRecoveredOrderTaxRateUpsertion(recoveredTaxRateId, upsertion)
      }
    }
  }

  private def recoverTaxRateId(taxRates: Seq[TaxRateRecord], taxRateId: UUID): ErrorsOr[Option[UUID]] =
    taxRates.find(_.id == taxRateId) match {
      case Some(taxRate) => Multiple.successOpt(taxRate.id)
      case None          => Multiple.failure(InvalidTaxRateIds(Seq(taxRateId)))
    }

  private def toRecoveredOrderTaxRateUpsertion(
      recoveredTaxRateId: Option[UUID],
      upsertion: OrderTaxRateUpsertion,
    ): RecoveredOrderTaxRateUpsertion =
    RecoveredOrderTaxRateUpsertion(
      taxRateId = recoveredTaxRateId,
      name = upsertion.name,
      value = upsertion.value,
      totalAmount = upsertion.totalAmount,
    )
}

final case class RecoveredOrderTaxRateUpsertion(
    taxRateId: Option[UUID],
    name: String,
    value: BigDecimal,
    totalAmount: BigDecimal,
  )
