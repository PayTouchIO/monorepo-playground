package io.paytouch.core.data.daos

import java.time.{ LocalDateTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.upsertions.CustomerUpsertion
import io.paytouch.core.data.model.{ CustomerMerchantRecord, CustomerMerchantUpdate }
import io.paytouch.core.data.tables.CustomerMerchantsTable
import io.paytouch.core.entities.enums.CustomerSource
import io.paytouch.core.filters.CustomerFilters
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime
import slick.lifted.CanBeQueryCondition

import scala.concurrent._

class CustomerMerchantDao(
    customerGroupDao: => CustomerGroupDao,
    customerLocationDao: => CustomerLocationDao,
    val globalCustomerDao: GlobalCustomerDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao {

  type Record = CustomerMerchantRecord
  type Update = CustomerMerchantUpdate
  type Table = CustomerMerchantsTable
  type Filters = CustomerFilters

  val table = TableQuery[Table]

  override def idColumnSelector: Table => Rep[UUID] = _.customerId

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId = merchantId, f.locationId, f.groupId, f.query, f.source, f.updatedSince)(
      offset,
      limit,
    )

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      groupId: Option[UUID],
      query: Option[String],
      source: Option[Seq[CustomerSource]],
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    run(
      queryFindAllByMerchantId(merchantId, locationId, groupId, query, source, updatedSince)
        .drop(offset)
        .take(limit)
        .result,
    )

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationId, f.groupId, f.query, f.source, f.updatedSince)

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      groupId: Option[UUID],
      query: Option[String],
      source: Option[Seq[CustomerSource]],
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, locationId, groupId, query, source, updatedSince).length.result)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID] = None,
      groupId: Option[UUID] = None,
      query: Option[String] = None,
      source: Option[Seq[CustomerSource]] = None,
      updatedSince: Option[ZonedDateTime],
    ) =
    querySearchByName(query)
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          locationId.map(lId =>
            t.customerId in customerLocationDao
              .queryFindByLocationIdAndMerchantId(lId, merchantId)
              .map(_.customerId),
          ),
          groupId.map(gId =>
            t.customerId in customerGroupDao.queryFindByGroupIdAndMerchantId(gId, merchantId).map(_.customerId),
          ),
          source.map(s => t.source inSet s),
          updatedSince.map { date =>
            any(
              t.id in queryUpdatedSince(date).map(_.id),
              t.customerId in customerLocationDao
                .queryUpdatedSince(date)
                .filter(_.merchantId === merchantId)
                .map(_.customerId),
            )
          },
        ),
      )
      .sortBy(c => (c.lastName.asc, c.firstName.asc))

  def findByCustomerIdsAndMerchantId(customerIds: Seq[UUID], merchantId: UUID): Future[Seq[Record]] =
    if (customerIds.isEmpty)
      Future.successful(Seq.empty)
    else
      run(queryFindByCustomerIdsAndMerchantId(customerIds, merchantId).result)

  private def queryFindOneByCustomerIdAndMerchantId(customerId: Option[UUID], merchantId: Option[UUID]) =
    table
      .filter(_.customerId === customerId && customerId.isDefined)
      .filter(_.merchantId === merchantId && merchantId.isDefined)

  def queryFindByCustomerIdsAndMerchantId(customerIds: Seq[UUID], merchantId: UUID) =
    table.filter(_.customerId inSet customerIds).filter(_.merchantId === merchantId)

  def querySearchByName(query: Option[String]) =
    query match {
      case Some(q) =>
        table.filter(t =>
          (t.firstName.toLowerCase.getOrElse("") ++
            " " ++
            t.lastName.toLowerCase.getOrElse("")).trim like s"%${q.toLowerCase}%",
        )
      case None => table
    }

  override def queryByIds(ids: Seq[UUID]) =
    throw new IllegalStateException(
      "Cannot find a customer merchant only by customer id. I need a merchant id as well!",
    )

  def findByMerchantIdAndCustomerId(merchantId: UUID, customerId: UUID): Future[Option[Record]] =
    findByCustomerIdsAndMerchantId(Seq(customerId), merchantId).map(_.headOption)

  override def queryUpsert(entityUpdate: Update) = {
    val customerId = entityUpdate.customerId
    val merchantId = entityUpdate.merchantId
    queryUpsertByQuery(entityUpdate, queryFindOneByCustomerIdAndMerchantId(customerId, merchantId))
  }

  override def queryDeleteTheRestByDeleteFilter[R <: Rep[_]](
      validEntities: Seq[Record],
      deleteFilter: CustomerMerchantsTable => R,
    )(implicit
      wt: CanBeQueryCondition[R],
    ) =
    table
      .filter(deleteFilter)
      .filterNot(_.customerId inSet validEntities.map(_.customerId))
      .filterNot(_.merchantId inSet validEntities.map(_.merchantId))
      .delete

  def upsert(upsertion: CustomerUpsertion): Future[(ResultType, Record)] =
    runWithTransaction(queryUpsertCustomerUpsertion(upsertion))

  def queryUpsertCustomerUpsertion(upsertion: CustomerUpsertion) =
    for {
      (_, globalCustomer) <- globalCustomerDao.queryUpsert(upsertion.globalCustomer)
      customerMerchantWithResultType <- queryUpsert(
        upsertion.customerMerchant.copy(customerId = Some(globalCustomer.id)),
      )
    } yield customerMerchantWithResultType

  def queryFindByMerchantId(
      merchantId: UUID,
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
    ) =
    table.filter(t =>
      all(
        Some(t.merchantId === merchantId),
        from.map(start => t.createdAt.asColumnOf[LocalDateTime] >= start),
        to.map(end => t.createdAt.asColumnOf[LocalDateTime] < end),
      ),
    )

  def markAsUpdatedById(id: UUID) =
    run(queryMarkAsUpdatedByIds(Seq(id)))
}
