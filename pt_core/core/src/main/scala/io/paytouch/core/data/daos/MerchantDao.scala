package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickUpsertDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.DynamicSortBySupport._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ BusinessType, LoadingStatus, PaymentProcessor }
import io.paytouch.core.data.model.upsertions.MerchantUpsertion
import io.paytouch.core.data.tables.MerchantsTable
import io.paytouch.core.entities.{ MerchantContext, MerchantSetupStep }
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.filters.MerchantFilters
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime

final class MerchantDao(
    val userRoleDao: UserRoleDao,
    val userDao: UserDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickUpsertDao
       with LazyLogging {
  type Record = MerchantRecord
  type Update = MerchantUpdate
  type Table = MerchantsTable
  type Upsertion = MerchantUpdate
  type Filters = MerchantFilters

  val table = TableQuery[Table]

  def create(upsertion: MerchantUpsertion): Future[(ResultType, Record, UserRecord)] =
    (for {
      (resultType, merchantRecord) <- queryUpsert(upsertion.merchant)
      userRolesWithResultType <- userRoleDao.queryBulkUpsert(upsertion.userRoles)
      userRoles = userRolesWithResultType.map { case (_, role) => role }
      (_, ownerUser) <- userDao.queryUpsert(
        upsertion.ownerUser.copy(userRoleId = adminUserRoleId(upsertion, userRoles)),
      )
    } yield (resultType, merchantRecord, ownerUser)).pipe(runWithTransaction)

  private def adminUserRoleId(upsertion: MerchantUpsertion, userRoles: Seq[UserRoleRecord]): Option[UUID] =
    upsertion
      .merchant
      .setupType
      .flatMap { setupType =>
        userRoles.find { role =>
          role.name == {
            setupType match {
              case enums.SetupType.Dash     => UserRoleUpdate.Manager
              case enums.SetupType.Paytouch => UserRoleUpdate.Admin
            }
          }
        }
      }
      .map(_.id)

  def updateSetupSteps(
      merchantId: UUID,
      setupCompleted: Boolean,
      updatedSteps: Map[MerchantSetupSteps, MerchantSetupStep],
    ): Future[Boolean] =
    table
      .filter(_.id === merchantId)
      .map(m => (m.setupCompleted, m.setupSteps, m.updatedAt))
      .update(setupCompleted, updatedSteps.some, UtcTime.now)
      .map(_ > 0)
      .pipe(runWithTransaction)

  def markLoadingAsInProgress(merchantId: UUID): Future[Boolean] =
    markLoadingStatus(merchantId, LoadingStatus.InProgress)

  def markLoadingAsSuccessful(merchantId: UUID): Future[Boolean] =
    markLoadingStatus(merchantId, LoadingStatus.Successful)

  def markLoadingAsFailed(merchantId: UUID): Future[Boolean] =
    markLoadingStatus(merchantId, LoadingStatus.Failed)

  private def markLoadingStatus(merchantId: UUID, loadingStatus: LoadingStatus): Future[Boolean] = {
    logger.info(s"[Merchant $merchantId] loading status to ${loadingStatus.entryName}")
    val field = for { o <- table if o.id === merchantId } yield (o.loadingStatus, o.updatedAt)
    runWithTransaction(field.update(loadingStatus, UtcTime.now).map(_ > 0))
  }

  def linkSwitchMerchants(merchantAId: UUID, merchantBId: UUID): Future[Boolean] =
    for {
      linkA <- linkSwitchMerchantId(merchantAId, merchantBId)
      linkB <- linkSwitchMerchantId(merchantBId, merchantAId)
    } yield linkA && linkB

  private def linkSwitchMerchantId(merchantId: UUID, switchMerchantId: UUID): Future[Boolean] = {
    val field = for { o <- table if o.id === merchantId } yield (o.switchMerchantId, o.updatedAt)
    runWithTransaction(field.update(Some(switchMerchantId), UtcTime.now)).map(_ > 0)
  }

  def findMerchantContextById(id: UUID): Future[Option[MerchantContext]] = {
    val q = table.filter(_.id === id).map(_.merchantContext)
    run(q.result.headOption)
  }

  def findAllWithFilters(f: Filters)(s: Sortings)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(
      queryFindAll(businessType = f.businessType, query = f.query, ids = f.ids)
        .dynamicSortBy(s, _.businessName.asc)
        .drop(offset)
        .take(limit)
        .result,
    )

  def countAllWithFilters(f: Filters): Future[Int] =
    run(queryFindAll(businessType = f.businessType, query = f.query, ids = f.ids).length.result)

  def queryFindAll(
      businessType: Option[BusinessType],
      query: Option[String],
      ids: Option[Seq[UUID]],
    ) =
    table.filter(t =>
      all(
        ids.map(i => t.id inSet i),
        businessType.map(bt => t.businessType === bt),
        query.map(q =>
          any(
            querySearchByName(t, q),
            querySearchByOwnerEmail(t, q),
          ),
        ),
      ),
    )

  def querySearchByName(t: Table, query: String) =
    t.businessName.trim ilike s"%${query}%"

  def querySearchByOwnerEmail(t: Table, query: String) =
    t.id in userDao.querySearchOwnersByEmail(query).map(_.merchantId)

  def updatePaymentProcessorConfig(
      id: MerchantIdPostgres,
      processor: PaymentProcessor,
      config: PaymentProcessorConfig,
    ): Future[Boolean] =
    table
      .filter(_.id === id.value)
      .map(o => o.paymentProcessor -> o.paymentProcessorConfig)
      .update(processor, config)
      .map(_ > 0)
      .pipe(runWithTransaction)
}
