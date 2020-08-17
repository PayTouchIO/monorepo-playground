package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ Permission, UserRecord, UserUpdate }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.UserUpsertion
import io.paytouch.core.data.tables.UsersTable
import io.paytouch.core.entities.{ UpdateActiveItem, UserInfo, UserLogin }
import io.paytouch.core.filters.UserFilters
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.utils.Multiple.ErrorsOr

class UserDao(
    val imageUploadDao: ImageUploadDao,
    val sessionDao: SessionDao,
    val userLocationDao: UserLocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickUpsertDao
       with SlickSoftDeleteDao
       with SlickToggleableItemDao {
  type Record = UserRecord
  type Update = UserUpdate
  type Upsertion = UserUpsertion
  type Filters = UserFilters
  type Table = UsersTable

  val table = TableQuery[Table]

  val nonDisabledTable = nonDeletedTable.filter(_.active === true)
  val deletableTable = table.filter(_.isOwner === false)

  def findUserLoginByEmail(email: String): Future[Option[UserLogin]] =
    table
      .filter(_.email.toLowerCase === email.toLowerCase)
      .map(_.userLogin)
      .result
      .headOption
      .pipe(run)

  def findUserLoginById(id: UUID): Future[Option[UserLogin]] =
    table
      .filter(_.id === id)
      .map(_.userLogin)
      .result
      .headOption
      .pipe(run)

  def findActiveUserLoginById(id: UUID): Future[Option[UserLogin]] =
    nonDisabledTable
      .filter(_.id === id)
      .map(_.userLogin)
      .result
      .headOption
      .pipe(run)

  def findUserLoginByAuth0UserId(auth0UserId: Auth0UserId): Future[Option[UserLogin]] =
    nonDisabledTable
      .filter(_.auth0UserId === auth0UserId.value)
      .map(_.userLogin)
      .result
      .headOption
      .pipe(run)

  // NOTE: user info considers all users, even deleted ones.
  def findUserInfoByIds(ids: Seq[UUID]): Future[Seq[UserInfo]] =
    table
      .filter(_.id inSet ids)
      .map(_.userInfo)
      .result
      .pipe(run)

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(
      merchantId = merchantId,
      locationIds = f.locationIds,
      userRoleId = f.userRoleId,
      query = f.query,
      updatedSince = f.updatedSince,
    )(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      userRoleId: Option[UUID],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(
      merchantId = merchantId,
      locationIds = locationIds,
      userRoleId = userRoleId,
      query = query,
      updatedSince = updatedSince,
    ).drop(offset).take(limit).result.pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(
      merchantId = merchantId,
      locationIds = f.locationIds,
      userRoleId = f.userRoleId,
      query = f.query,
      updatedSince = f.updatedSince,
    )

  def countAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      userRoleId: Option[UUID],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] =
    queryFindAllByMerchantId(
      merchantId = merchantId,
      locationIds = locationIds,
      userRoleId = userRoleId,
      query = query,
      updatedSince = updatedSince,
    ).length.result.pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      userRoleId: Option[UUID],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
    ) =
    nonDeletedTable.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        Some(t.id in userLocationDao.queryFindByLocationIds(locationIds).map(_.userId)),
        userRoleId.map(uRId => (t.userRoleId === uRId).getOrElse(false)),
        query.map(q => querySearchUser(t, q)),
        updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
      )
    }

  private def querySearchUser(t: Table, q: String) =
    querySearchByFirstName(t, q) || querySearchByLastName(t, q) || querySearchByEmail(t, q)

  private def querySearchByFirstName(t: Table, q: String) =
    t.firstName.toLowerCase like s"%${q.toLowerCase}%"

  private def querySearchByLastName(t: Table, q: String) =
    t.lastName.toLowerCase like s"%${q.toLowerCase}%"

  private def querySearchByEmail(t: Table, q: String) =
    t.email.toLowerCase like s"%${q.toLowerCase}%"

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] =
    (for {
      (resultType, user) <- queryUpsert(upsertion.user)
      _ <- userLocationDao.queryBulkUpsertAndDeleteTheRest(upsertion.userLocations, user.id)
      _ <- asOption(upsertion.imageUploadUpdates.map { img =>
        val imgType = ImageUploadType.User
        val userIds = Seq(user.id)
        imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(img, userIds, imgType)
      })
    } yield (resultType, user)).pipe(runWithTransaction)

  def recordDashboardLogin(email: String, date: ZonedDateTime): Future[Boolean] =
    table
      .withFilter(_.email === email)
      .map(o => o.dashboardLastLoginAt -> o.updatedAt)
      .update(Some(date), UtcTime.now)
      .map(_ > 0)
      .pipe(run)

  def recordRegisterLogin(email: String, date: ZonedDateTime): Future[Boolean] =
    table
      .withFilter(_.email === email)
      .map(o => o.registerLastLoginAt -> o.updatedAt)
      .update(Some(date), UtcTime.now)
      .map(_ > 0)
      .pipe(run)

  def recordTicketsLogin(email: String, date: ZonedDateTime): Future[Boolean] =
    table
      .withFilter(_.email === email)
      .map(o => o.ticketsLastLoginAt -> o.updatedAt)
      .update(Some(date), UtcTime.now)
      .map(_ > 0)
      .pipe(run)

  def findByEmail(email: String): Future[Option[Record]] =
    table
      .filter(_.email === email)
      .result
      .headOption
      .pipe(run)

  def countByUserRoleIds(userRoleIds: Seq[UUID]): Future[Map[UUID, Int]] =
    queryFindByUserRoleIds(userRoleIds)
      .groupBy(_.userRoleId)
      .map { case (userRoleId, rows) => userRoleId -> rows.size }
      .result
      .pipe(run)
      .map(_.toMap.flatMap { case (k, v) => k.map(_ -> v) })

  def queryFindByUserRoleIds(userRoleIds: Seq[UUID]) =
    table.filter(_.userRoleId inSet userRoleIds)

  def queryFindByUserRoleId(userRoleId: UUID) =
    queryFindByUserRoleIds(Seq(userRoleId))

  def findByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]) =
    queryFindByIdsAndLocationIds(ids, locationIds)
      .result
      .pipe(run)

  def queryFindByIdsAndLocationIds(ids: Seq[UUID], locationIds: Seq[UUID]) =
    queryFindByIds(ids)
      .filter(_.id in userLocationDao.queryFindByLocationIds(locationIds).map(_.userId))

  def querySearchByFullName(q: String) =
    table.filter(t => (t.firstName.toLowerCase ++ " " ++ t.lastName.toLowerCase) like s"%${q.toLowerCase}%")

  def querySearchOwnersByEmail(query: String) =
    table
      .filter(_.email ilike s"%$query%")
      .filter(_.isOwner === true)

  override def queryDeleteByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID) = {
    val now = UtcTime.now

    deletableTable
      .withFilter(o => o.id.inSet(ids) && o.merchantId === merchantId)
      .map(o => (o.pin, o.deletedAt, o.updatedAt))
      .update(None, now.some, now)
      .map(_ => ids)
  }

  override def deleteByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID): Future[Seq[UUID]] =
    if (ids.isEmpty)
      Future.successful(Seq.empty)
    else
      (for {
        result <- queryDeleteByIdsAndMerchantId(ids, merchantId)
        _ <- sessionDao.queryDeleteByUserIds(ids)
      } yield result).pipe(runWithTransaction)

  override def bulkUpdateActiveField(merchantId: UUID, updates: Seq[UpdateActiveItem]): Future[Unit] = {
    val idsToActivate = updates.filter(_.active == true).map(_.itemId)
    val idsToDisactivate = updates.filter(_.active == false).map(_.itemId)

    (for {
      _ <- querySetActive(merchantId, idsToActivate, true)
      _ <- querySetActive(merchantId, idsToDisactivate, false)
      _ <- sessionDao.queryDeleteByUserIds(idsToDisactivate)
    } yield ()).pipe(run)
  }

  def findOwnersByMerchantId(merchantId: UUID): Future[Seq[Record]] =
    findOwnersByMerchantIds(Seq(merchantId))
      .map(_.getOrElse(merchantId, Seq.empty))

  def findOwnersByMerchantIds(merchantIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] =
    table
      .filter(_.merchantId inSet merchantIds)
      .filter(_.isOwner === true)
      .result
      .pipe(run)
      .map(result => result.groupBy(_.merchantId))

  override def querySetActive(
      merchantId: UUID,
      ids: Seq[UUID],
      active: Boolean,
    ) =
    deletableTable
      .withFilter(o => o.id.inSet(ids) && o.merchantId === merchantId)
      .map(o => o.active -> o.updatedAt)
      .update(active, UtcTime.now)

  def renameEmailsByMerchantId(merchantId: UUID, prefix: String) =
    sqlu"""UPDATE #${table.baseTableRow.tableName}
            SET email = CONCAT($prefix, email),
                updated_at = '#${UtcTime.now}'
            WHERE merchant_id = $merchantId""".pipe(runWithTransaction)

  def setEncryptedPasswordAndPin(
      id: UUID,
      encryptedPassword: String,
      encryptedPin: Option[String],
    ) =
    table
      .withFilter(_.id === id)
      .map(o => (o.password, o.pin, o.updatedAt))
      .update(encryptedPassword, encryptedPin, UtcTime.now)
      .pipe(runWithTransaction)

  def findByMerchantIdAndEncryptedPin(merchantId: UUID, pin: String): Future[Seq[Record]] =
    queryFindByMerchantIdAndEncryptedPin(merchantId, pin)
      .result
      .pipe(run)

  def findOneByMerchantIdAndEncryptedPin(merchantId: UUID, pin: String): Future[Option[Record]] =
    queryFindByMerchantIdAndEncryptedPin(merchantId, pin)
      .result
      .headOption
      .pipe(run)

  private def queryFindByMerchantIdAndEncryptedPin(merchantId: UUID, pin: String) =
    nonDeletedTable
      .filter(_.merchantId === merchantId)
      .filter(_.pin === pin)

  def findUsersWithPermission(
      merchantId: UUID,
      target: String,
      permissionName: String,
      permissionToFilterFor: Permission,
    ): Future[Seq[Record]] = {
    import UsersTable.getResultUserRecord

    val permissionMask = permissionToFilterFor.representation
    val fields = table.baseTableRow.create_*.map(f => s"u.${f.name}").mkString(",")

    sql"""SELECT #$fields
            FROM #${table.baseTableRow.tableName} u
            JOIN user_roles ur
            ON u.user_role_id = ur.id
            WHERE (ur.#$target->>'#$permissionName')::bit(4) & B'#$permissionMask' = B'#$permissionMask'
            AND u.merchant_id = $merchantId
            AND u.active = 't'
            AND u.deleted_at IS NULL"""
      .as[UserRecord]
      .pipe(run)
  }
}
