package io.paytouch.core.services

import java.util.UUID

import cats.data.OptionT
import cats.instances.future._
import io.paytouch.core.conversions.CashDrawerConversions
import io.paytouch.core.data.daos.{ CashDrawerDao, Daos }
import io.paytouch.core.data.model.enums.{ CashDrawerActivityType, CashDrawerStatus }
import io.paytouch.core.data.model.{ CashDrawerRecord, Permission }
import io.paytouch.core.data.model.upsertions.{ CashDrawerUpsertion => CashDrawerUpsertionModel }
import io.paytouch.core.entities.{ CashDrawer => CashDrawerEntity, CashDrawerUpsertion => CashDrawerUpsertionEntity, _ }
import io.paytouch.core.expansions.{ LocationExpansions, MerchantExpansions, NoExpansions }
import io.paytouch.core.filters.CashDrawerFilters
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.features.{ FindAllFeature, FindByIdFeature }
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.{ CashDrawerRecoveryValidator, CashDrawerValidator }

import scala.concurrent._

class CashDrawerService(
    merchantService: MerchantService,
    locationService: LocationService,
    locationReceiptService: LocationReceiptService,
    userService: UserService,
    messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends CashDrawerConversions
       with FindByIdFeature
       with FindAllFeature {

  type Dao = CashDrawerDao
  type Entity = CashDrawerEntity
  type Expansions = NoExpansions
  type Filters = CashDrawerFilters
  type Record = CashDrawerRecord
  type Validator = CashDrawerValidator

  protected val dao = daos.cashDrawerDao
  protected val validator = new CashDrawerValidator
  val recoveryValidator = new CashDrawerRecoveryValidator

  val defaultFilters = CashDrawerFilters()

  def enrich(records: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    Future.successful(records.map(fromRecordToEntity))

  def syncById(
      id: UUID,
      upsertion: CashDrawerUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    recoverUpsertionModel(id, upsertion).flatMap { upsertion =>
      for {
        (resultType, record) <- dao.upsert(upsertion)
        entity = fromRecordToEntity(record)
        _ <- sendSyncedMessage(entity)
        _ <-
          if (entity.status == CashDrawerStatus.Ended) prepareCashDrawerReport(entity)
          else Future.unit
      } yield (resultType, entity)
    }

  private def recoverUpsertionModel(
      id: UUID,
      upsertion: CashDrawerUpsertionEntity,
    )(implicit
      user: UserContext,
    ): Future[CashDrawerUpsertionModel] =
    recoveryValidator.recoverUpsertion(id, upsertion)

  private def prepareCashDrawerReport(entity: Entity)(implicit user: UserContext): Future[Unit] = {
    val optT = for {
      merchant <- OptionT(merchantService.findById(user.merchantId)(MerchantExpansions.none))
      permissionToFilterFor = Permission(create = true)
      targetUsers <- OptionT.liftF(
        userService.findUsersWithPermission("register", "cashDrawers", permissionToFilterFor),
      )
      locationId <- OptionT.fromOption[Future](entity.locationId)
      location <- OptionT(locationService.findById(locationId)(LocationExpansions.empty))
      locationReceipt <- OptionT(locationReceiptService.findByLocationId(locationId))
      cashier <- OptionT(userService.getUserInfoByIds(entity.userId.toSeq).map(_.headOption))
    } yield messageHandler.prepareCashDrawerReport(entity, merchant, targetUsers, location, locationReceipt, cashier)
    optT.value.map(_.getOrElse((): Unit))
  }

  def storeExportS3Filename(id: UUID, filename: String): Future[Boolean] =
    dao.storeExportS3Filename(id, filename)

  def listReasons()(implicit user: UserContext): Future[Seq[CashDrawerReason]] =
    Future.successful {
      defaultReasonsMap.flatMap {
        case (key, reasons) =>
          reasons.zipWithIndex.map {
            case ((id, reasonText), position) =>
              CashDrawerReason(id = UUID.fromString(id), position = position, reasonText = reasonText, `type` = key)
          }
      }.toSeq
    }

  private lazy val defaultReasonsMap: Map[CashDrawerActivityType, Seq[(String, String)]] = Map(
    CashDrawerActivityType.PayIn -> Seq(
      "67cd6fa6-dfd7-4939-907b-2ce67bd02e38" -> "Additional Cash for Drawer",
      "22311e34-a3bb-4897-93b3-1c1fcea6530c" -> "Other",
    ),
    CashDrawerActivityType.PayOut -> Seq(
      "ae87e99e-92b6-4718-9212-9207fa2d0a3e" -> "Supplier Payment",
      "bb08dc70-01b4-4f9b-9596-443d68277599" -> "Vendor Payment",
      "fa62c124-37da-49d7-b489-468032e8c580" -> "Bill Payment",
      "ad984390-5c6a-41cd-91d2-f17b46a6cd91" -> "Other",
    ),
    CashDrawerActivityType.NoSale -> Seq(
      "7141f917-7b4b-4986-9151-a80750ee2519" -> "Cash Change",
      "c95e82b1-3473-4be7-9093-719be5d983d7" -> "Cash Count",
      "28b78a77-31f7-42e1-89fb-f23690325929" -> "Other",
    ),
  )

  private def sendSyncedMessage(entity: Entity)(implicit user: UserContext) =
    Future.successful(
      entity.locationId.foreach(locationId => messageHandler.sendEntitySynced(entity, locationId)),
    )

  def findRecordById(id: UUID) = dao.findById(id)

  def sendReport(id: UUID)(implicit user: UserContext): Future[ErrorsOr[Unit]] =
    validator.accessOneById(id).flatMapTraverse(cashDrawer => prepareCashDrawerReport(fromRecordToEntity(cashDrawer)))
}
