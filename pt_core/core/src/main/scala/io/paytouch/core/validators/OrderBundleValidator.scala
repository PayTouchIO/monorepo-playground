package io.paytouch.core.validators

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, OrderBundleDao }
import io.paytouch.core.data.model.OrderBundleRecord
import io.paytouch.core.entities._
import io.paytouch.core.errors.{ InvalidOrderBundleIds, NonAccessibleOrderBundleIds }
import io.paytouch.core.utils.Multiple.ErrorsOr

import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import io.paytouch.core.validators.features.DefaultRecoveryValidator

import scala.concurrent._

class OrderBundleValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends DefaultRecoveryValidator[OrderBundleRecord] {

  type Record = OrderBundleRecord
  type Dao = OrderBundleDao

  protected val dao = daos.orderBundleDao
  val validationErrorF = InvalidOrderBundleIds(_)
  val accessErrorF = NonAccessibleOrderBundleIds(_)

  val bundleSetValidator = new BundleSetValidator()
  val bundleOptionValidator = new BundleOptionValidator()
  val orderItemValidator = new OrderItemValidator()
  val orderBundleItemCostCalculation = new OrderBundleItemCostCalculation()

  def validateUpsertions(
      orderId: UUID,
      upsertions: Seq[OrderBundleUpsertion],
      orderItemIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[OrderBundleUpsertion]]] = {
    val orderBundleIds = upsertions.map(_.id)
    val bundleSetIds = upsertions.flatMap(_.orderBundleSets.map(_.bundleSetId))
    val bundleOptionIds = upsertions.flatMap(_.orderBundleSets.flatMap(_.orderBundleOptions).map(_.bundleOptionId))
    for {
      validBundleIds <- filterNonAlreadyTakenIds(orderBundleIds)
      validBundleSetIds <- bundleSetValidator.filterValidByIds(bundleSetIds).map(_.map(_.id))
      validBundleOptionIds <- bundleOptionValidator.filterValidByIds(bundleOptionIds).map(_.map(_.id))
    } yield Multiple.combineSeq(
      upsertions.map { upsertion =>
        val validOrderBundleId = recoverOrderBundleId(validBundleIds, upsertion.id)
        val validBundleOrderItemId = recoverOrderItemId(orderItemIds, upsertion.bundleOrderItemId)
        val validOrderBundleSets =
          validateOrderBundleSets(upsertion.orderBundleSets, orderItemIds, validBundleSetIds, validBundleOptionIds)
        Multiple.combine(validOrderBundleId, validBundleOrderItemId, validOrderBundleSets) {
          case _ => upsertion
        }
      },
    )
  }

  def recoverUpsertions(
      orderId: UUID,
      upsertions: Seq[OrderBundleUpsertion],
      orderItemUpsertions: Seq[RecoveredOrderItemUpsertion],
    )(implicit
      user: UserContext,
    ): Future[Seq[RecoveredOrderBundle]] = {
    val orderBundleIds = upsertions.map(_.id)
    val orderItemIds = orderItemUpsertions.map(_.id)
    val bundleSetIds = upsertions.flatMap(_.orderBundleSets.map(_.bundleSetId))
    val bundleOptionIds = upsertions.flatMap(_.orderBundleSets.flatMap(_.orderBundleOptions).map(_.bundleOptionId))
    for {
      validBundleIds <- filterNonAlreadyTakenIds(orderBundleIds)
      validBundleSetIds <- bundleSetValidator.filterValidByIds(bundleSetIds).map(_.map(_.id))
      validBundleOptionIds <- bundleOptionValidator.filterValidByIds(bundleOptionIds).map(_.map(_.id))
    } yield upsertions.flatMap { upsertion =>
      val description = s"While validating bundle ${upsertion.id} for order $orderId"

      val recoveredOrderBundleId: UUID =
        logger.loggedSoftRecoverUUID(recoverOrderBundleId(validBundleIds, upsertion.id))("Generating new bundle id")
      val recoveredBundleOrderItemId =
        logger.loggedRecover(recoverOrderItemId(orderItemIds, upsertion.bundleOrderItemId))(description, upsertion)

      if (upsertion.orderBundleSets.isEmpty)
        logger.warn(s"$description orderBundleSets is empty or missing")

      recoveredBundleOrderItemId.map { bundleOrderItemId =>
        RecoveredOrderBundle(
          id = recoveredOrderBundleId,
          bundleOrderItemId = bundleOrderItemId,
          orderBundleSets =
            recoverOrderBundleSets(upsertion.orderBundleSets, orderItemIds, validBundleSetIds, validBundleOptionIds),
        )
      }
    }
  }

  def recoverBundleItemCost(
      bundles: Seq[RecoveredOrderBundle],
      items: Seq[RecoveredOrderItemUpsertion],
    ): Future[Seq[RecoveredOrderItemUpsertion]] =
    orderBundleItemCostCalculation.recoverBundleItemCost(bundles, items)

  private def validateOrderBundleSets(
      orderBundleSets: Seq[OrderBundleSetUpsertion],
      orderItemIds: Seq[UUID],
      validBundleSetIds: Seq[UUID],
      validBundleOptionIds: Seq[UUID],
    ): ErrorsOr[Seq[OrderBundleSetUpsertion]] =
    Multiple.combineSeq(orderBundleSets.map { orderBundleSet =>
      val validBundleSetId = recoverBundleSetId(validBundleSetIds, orderBundleSet.bundleSetId)
      val validOrderBundleOptions =
        validateOrderBundleOptions(orderBundleSet.orderBundleOptions, orderItemIds, validBundleOptionIds)
      Multiple.combine(validBundleSetId, validOrderBundleOptions) { case _ => orderBundleSet }
    })

  private def recoverOrderBundleSets(
      orderBundleSets: Seq[OrderBundleSetUpsertion],
      orderItemIds: Seq[UUID],
      validBundleSetIds: Seq[UUID],
      validBundleOptionIds: Seq[UUID],
    ): Seq[RecoveredOrderBundleSet] =
    orderBundleSets.map { orderBundleSet =>
      val recoveredBundleSetId: Option[UUID] = logger.loggedSoftRecover(
        recoverBundleSetId(validBundleSetIds, orderBundleSet.bundleSetId),
      )("Setting bundle set id to None")
      RecoveredOrderBundleSet(
        id = orderBundleSet.id,
        bundleSetId = recoveredBundleSetId,
        name = orderBundleSet.name,
        position = orderBundleSet.position,
        orderBundleOptions =
          recoverOrderBundleOptions(orderBundleSet.orderBundleOptions, orderItemIds, validBundleOptionIds),
      )
    }

  private def validateOrderBundleOptions(
      orderBundleOptions: Seq[OrderBundleOptionUpsertion],
      orderItemIds: Seq[UUID],
      validBundleOptionIds: Seq[UUID],
    ): ErrorsOr[Seq[OrderBundleOptionUpsertion]] =
    Multiple.combineSeq(orderBundleOptions.map { orderBundleOption =>
      val validArticleOrderItemId = recoverOrderItemId(orderItemIds, orderBundleOption.articleOrderItemId)
      val validBundleOptionId = recoverBundleOptionId(validBundleOptionIds, orderBundleOption.bundleOptionId)
      Multiple.combine(validArticleOrderItemId, validBundleOptionId) { case _ => orderBundleOption }
    })

  private def recoverOrderBundleOptions(
      orderBundleOptions: Seq[OrderBundleOptionUpsertion],
      orderItemIds: Seq[UUID],
      validBundleOptionIds: Seq[UUID],
    ): Seq[RecoveredOrderBundleOption] =
    orderBundleOptions.map { orderBundleOption =>
      val description = s"While validating bundle option ${orderBundleOption.id}"
      val recoveredArticleOrderItemId =
        logger.loggedRecover(recoverOrderItemId(orderItemIds, orderBundleOption.articleOrderItemId))(
          description,
          orderBundleOption,
        )
      val recoveredBundleOptionId: Option[UUID] =
        logger.loggedSoftRecover(recoverBundleOptionId(validBundleOptionIds, orderBundleOption.bundleOptionId))(
          "Setting bundle option id to None",
        )

      RecoveredOrderBundleOption(
        id = orderBundleOption.id,
        bundleOptionId = recoveredBundleOptionId,
        articleOrderItemId = recoveredArticleOrderItemId,
        position = orderBundleOption.position,
        priceAdjustment = orderBundleOption.priceAdjustment,
      )
    }

  private def recoverOrderBundleId(validBundleIds: Seq[UUID], bundleId: UUID): ErrorsOr[UUID] =
    if (validBundleIds.contains(bundleId)) Multiple.success(bundleId)
    else Multiple.failure(NonAccessibleOrderBundleIds(Seq(bundleId)))

  private def recoverOrderItemId(orderItemIds: Seq[UUID], orderItemId: UUID): ErrorsOr[Option[UUID]] =
    if (orderItemIds.contains(orderItemId)) Multiple.success(Some(orderItemId))
    else Multiple.failure(orderItemValidator.validationErrorF(Seq(orderItemId)))

  private def recoverBundleSetId(validBundleSetIds: Seq[UUID], bundleSetId: UUID): ErrorsOr[Option[UUID]] =
    if (validBundleSetIds.contains(bundleSetId)) Multiple.success(Some(bundleSetId))
    else Multiple.failure(bundleSetValidator.validationErrorF(Seq(bundleSetId)))

  private def recoverBundleOptionId(
      validBundleOptionIds: Seq[UUID],
      bundleOptionId: UUID,
    ): ErrorsOr[Option[UUID]] =
    if (validBundleOptionIds.contains(bundleOptionId)) Multiple.success(Some(bundleOptionId))
    else Multiple.failure(bundleOptionValidator.validationErrorF(Seq(bundleOptionId)))
}

final case class RecoveredOrderBundle(
    id: UUID,
    bundleOrderItemId: UUID,
    orderBundleSets: Seq[RecoveredOrderBundleSet],
  )

final case class RecoveredOrderBundleSet(
    id: UUID,
    bundleSetId: Option[UUID],
    name: Option[String],
    position: Option[Int],
    orderBundleOptions: Seq[RecoveredOrderBundleOption],
  )

final case class RecoveredOrderBundleOption(
    id: UUID,
    bundleOptionId: Option[UUID],
    articleOrderItemId: Option[UUID],
    position: Option[Int],
    priceAdjustment: BigDecimal,
  )

class OrderBundleItemCostCalculation(implicit val ec: ExecutionContext, val daos: Daos) {
  def recoverBundleItemCost(
      bundles: Seq[RecoveredOrderBundle],
      items: Seq[RecoveredOrderItemUpsertion],
    ): Future[Seq[RecoveredOrderItemUpsertion]] =
    items
      .toList
      .traverse { item =>
        bundles
          .find(_.bundleOrderItemId == item.id)
          .map(bundle => recoverBundleCost(item, bundle, items))
          .getOrElse(Future.successful(item))
      }

  private def recoverBundleCost(
      item: RecoveredOrderItemUpsertion,
      bundle: RecoveredOrderBundle,
      items: Seq[RecoveredOrderItemUpsertion],
    ): Future[RecoveredOrderItemUpsertion] =
    item
      .costAmount
      .filter(_ > 0)
      .as(Future.successful(item))
      .getOrElse {
        calculateItemCosts(bundleItems(bundle, items)).map(costAmount => item.copy(costAmount = Some(costAmount)))
      }

  private def bundleItems(
      bundle: RecoveredOrderBundle,
      items: Seq[RecoveredOrderItemUpsertion],
    ): Seq[RecoveredOrderItemUpsertion] = {
    val itemIds = getItemIds(bundle)

    items.filter(item => itemIds.contains(item.id))
  }

  private def getItemIds(bundle: RecoveredOrderBundle): Seq[UUID] =
    bundle
      .orderBundleSets
      .flatMap { orderBundleSet =>
        orderBundleSet
          .orderBundleOptions
          .toSeq
          .flatMap(_.articleOrderItemId)
      }

  private def calculateItemCosts(items: Seq[RecoveredOrderItemUpsertion]): Future[BigDecimal] =
    items
      .toList
      .traverse(item => itemCostAmount(item).map(itemCost => itemCost * itemQuantity(item)))
      .map(_.sum)

  private def itemQuantity(item: RecoveredOrderItemUpsertion): BigDecimal =
    item.quantity.getOrElse(1)

  private def itemCostAmount(item: RecoveredOrderItemUpsertion): Future[BigDecimal] =
    item
      .costAmount
      .filter(_ > 0)
      .map(Future.successful)
      .getOrElse(recoverItemCost(item))

  private def recoverItemCost(item: RecoveredOrderItemUpsertion): Future[BigDecimal] =
    item
      .productId
      .map(recoverProductCost)
      .getOrElse(Future.successful(0))

  private def recoverProductCost(productId: UUID): Future[BigDecimal] =
    daos
      .productDao
      .findById(productId)
      .map { articleRecord =>
        articleRecord
          .flatMap(_.costAmount)
          .getOrElse(0)
      }
}
