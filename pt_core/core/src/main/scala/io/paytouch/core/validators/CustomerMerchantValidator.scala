package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.errors._
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.validators.features._

class CustomerMerchantValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends Validator[CustomerMerchantRecord]
       with EmailValidator
       with DeletionValidator[CustomerMerchantRecord] {
  type Record = CustomerMerchantRecord
  type Dao = CustomerMerchantDao

  protected val dao = daos.customerMerchantDao
  val validationErrorF = InvalidCustomerIds(_)
  val accessErrorF = NonAccessibleCustomerIds(_)

  val loyaltyProgramValidator = new LoyaltyProgramValidator

  protected def recordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[Record]] =
    dao.findByCustomerIdsAndMerchantId(ids, user.merchantId)

  def validateCreateOrUpdate(
      optId: Option[UUID],
      update: CustomerMerchantUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[(CustomerMerchantUpsertion, Option[LoyaltyProgramRecord])]] =
    for {
      validId <- accessOneByOptId(optId)
      validSync <- validateSync(optId, update)
    } yield Multiple.combine(validId, validSync) { case (_, tuple) => tuple }

  def validateSync(
      optId: Option[UUID],
      update: CustomerMerchantUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[(CustomerMerchantUpsertion, Option[LoyaltyProgramRecord])]] =
    for {
      validEmail <- validateEmailFormat(update.email)
      validLoyaltyProgram <- loyaltyProgramValidator.accessOneByOptId(update.enrollInLoyaltyProgramId)
      validEmailAndLoyaltyProgram <- validateEmailToJoinLoyaltyProgram(optId, update)
    } yield Multiple.combine(validEmail, validLoyaltyProgram, validEmailAndLoyaltyProgram) {
      case (_, loyaltyProgram, _) => update -> loyaltyProgram
    }

  private def validateEmailToJoinLoyaltyProgram(
      optId: Option[UUID],
      update: CustomerMerchantUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[CustomerMerchantUpsertion]] =
    if (update.enrollInLoyaltyProgramId.isDefined)
      optId match {
        case Some(id)                       => recordsFinder(Seq(id)).map(r => validateEmailToJoinLoyaltyProgram(update, r.headOption))
        case None if update.email.isDefined => Future.successful(Multiple.success(update))
        case None                           => Future.successful(Multiple.failure(EmailRequiredForLoyaltySignUp()))
      }
    else Future.successful(Multiple.success(update))

  private def validateEmailToJoinLoyaltyProgram(
      update: CustomerMerchantUpsertion,
      record: Option[CustomerMerchantRecord],
    ): ErrorsOr[CustomerMerchantUpsertion] =
    if (record.flatMap(_.email).isEmpty && update.email.toOption.isEmpty)
      Multiple.failure(EmailRequiredForLoyaltySignUp())
    else
      Multiple.success(update)
}
