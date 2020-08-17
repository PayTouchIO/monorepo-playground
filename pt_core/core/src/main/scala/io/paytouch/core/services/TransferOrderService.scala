package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.TransferOrderConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.enums.ReceivingOrderObjectType
import io.paytouch.core.data.model.upsertions.TransferOrderUpsertion
import io.paytouch.core.data.model.{
  ReceivingOrderRecord,
  TransferOrderRecord,
  TransferOrderUpdate => TransferOrderUpdateModel,
}
import io.paytouch.core.entities.{
  Location,
  MonetaryAmount,
  TransferOrderCreation,
  UserContext,
  UserInfo,
  TransferOrder => TransferOrderEntity,
  TransferOrderUpdate => TransferOrderUpdateEntity,
}
import io.paytouch.core.expansions.TransferOrderExpansions
import io.paytouch.core.filters.TransferOrderFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.TransferOrderValidator

import scala.concurrent._

class TransferOrderService(
    val locationService: LocationService,
    val receivingOrderService: ReceivingOrderService,
    val transferOrderProductService: TransferOrderProductService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends TransferOrderConversions
       with FindByIdFeature
       with FindAllFeature
       with CreateAndUpdateFeature
       with ReceivingObjectUpdateService {

  type Creation = TransferOrderCreation
  type Dao = TransferOrderDao
  type Entity = TransferOrderEntity
  type Expansions = TransferOrderExpansions
  type Filters = TransferOrderFilters
  type Model = TransferOrderUpsertion
  type Record = TransferOrderRecord
  type Update = TransferOrderUpdateEntity
  type Validator = TransferOrderValidator

  protected val dao = daos.transferOrderDao
  protected val validator = new TransferOrderValidator
  val defaultFilters = TransferOrderFilters()
  val objectType = ReceivingOrderObjectType.Transfer

  def enrich(records: Seq[Record], f: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val fromLocationPerRecordR = getOptionalFromLocationPerTransferOrder(records)(e.withFromLocation)
    val toLocationPerRecordR = getOptionalToLocationPerTransferOrder(records)(e.withToLocation)
    val userPerRecordR = getOptionalUserPerTransferOrder(records)(e.withUser)
    val productCountPerRecordR = getOptionalProductsCountPerTransferOrder(records)(e.withProductsCount)
    val stockValuePerRecordR = getOptionalStockValuePerTransferOrder(records)(e.withStockValue)
    for {
      fromLocationPerRecord <- fromLocationPerRecordR
      toLocationPerRecord <- toLocationPerRecordR
      userPerRecord <- userPerRecordR
      productCountPerRecord <- productCountPerRecordR
      stockValuePerRecord <- stockValuePerRecordR
    } yield fromRecordsAndOptionsToEntities(
      records,
      fromLocationPerRecord,
      toLocationPerRecord,
      userPerRecord,
      productCountPerRecord,
      stockValuePerRecord,
    )
  }

  private def getOptionalFromLocationPerTransferOrder(
      records: Seq[Record],
    )(
      withFromLocation: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Location]] =
    getExpandedField[Location](
      locationService.findByIds,
      _.id,
      _.fromLocationId,
      records,
      withFromLocation,
    )

  private def getOptionalToLocationPerTransferOrder(
      records: Seq[Record],
    )(
      withToLocation: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Location]] =
    getExpandedField[Location](
      locationService.findByIds,
      _.id,
      _.toLocationId,
      records,
      withToLocation,
    )

  private def getOptionalUserPerTransferOrder(records: Seq[Record])(withUser: Boolean): Future[DataByRecord[UserInfo]] =
    getExpandedField[UserInfo](
      userService.getUserInfoByIds,
      _.id,
      _.userId,
      records,
      withUser,
    )

  private def getOptionalProductsCountPerTransferOrder(
      transferOrders: Seq[Record],
    )(
      withProductsCount: Boolean,
    ): Future[DataByRecord[BigDecimal]] =
    if (withProductsCount)
      transferOrderProductService.countProductsByTransferOrderIds(transferOrders).map(Some(_))
    else Future.successful(None)

  private def getOptionalStockValuePerTransferOrder(
      transferOrders: Seq[Record],
    )(
      withStockValue: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[MonetaryAmount]] =
    if (withStockValue)
      transferOrderProductService.findStockValueByTransferOrderIds(transferOrders).map(Some(_))
    else Future.successful(None)

  def findByIds(ids: Seq[UUID])(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] =
    for {
      records <- dao.findByIds(ids)
      entities <- enrich(records, defaultFilters)(e)
    } yield entities

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      transferOrder <- convertToTransferOrderUpdateModel(id, update)
      transferOrderProducts <- transferOrderProductService.convertToTransferOrderProductUpdates(id, update)
    } yield Multiple.combine(transferOrder, transferOrderProducts)(TransferOrderUpsertion)

  private def convertToTransferOrderUpdateModel(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[TransferOrderUpdateModel]] =
    validator.validateUpsertion(update).mapNested(_ => fromUpsertionToUpdate(id, update))

  protected def updatePaymentStatus(objectRecord: Dao#Record, receivingOrders: Seq[ReceivingOrderRecord]): Future[Int] =
    Future.successful(0)

}
