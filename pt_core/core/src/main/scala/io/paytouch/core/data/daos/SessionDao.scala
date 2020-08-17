package io.paytouch.core.data.daos

import java.time._
import java.util.UUID

import scala.concurrent._

import cats.implicits._

import slick.dbio.{ DBIOAction, NoStream }

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickCommonDao, SlickFindAllDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.SessionRecord
import io.paytouch.core.data.tables.SessionsTable
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.entities.JsonWebToken
import io.paytouch.core.filters.SessionFilters
import io.paytouch.core.utils.{ JwtTokenKeys, UtcTime }

class SessionDao(
    userDao: => UserDao,
    oauthAppDao: OAuthAppDao,
    oauthAppSessionDao: OAuthAppSessionDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with JwtTokenKeys
       with LazyLogging {
  type Record = SessionRecord
  type Table = SessionsTable
  type Filters = SessionFilters

  val table = TableQuery[Table]

  def create(
      userId: UUID,
      source: LoginSource,
      adminId: Option[UUID] = None,
    ) =
    insert(
      SessionRecord(
        id = UUID.randomUUID,
        userId = userId,
        jti = UUID.randomUUID.toString.replace("-", ""),
        source = source,
        adminId = adminId,
        createdAt = UtcTime.now,
        updatedAt = UtcTime.now,
      ),
    )

  def access(token: JsonWebToken): Future[Option[Record]] =
    findSessionRecord(token).map {
      _.map { matchingSession =>
        recordAccess(matchingSession)

        matchingSession
      }
    }

  def delete(token: JsonWebToken): Future[Unit] =
    access(token).flatMap(_.fold(Future.unit)(session => deleteById(session.id).void))

  private def insert(record: Record): Future[Record] =
    runWithTransaction(table.returning(table) += record)

  private def findOneMatchingTokenData(
      userId: UUID,
      jti: String,
      source: LoginSource,
      adminId: Option[UUID],
    ): Future[Option[Record]] =
    table
      .filter(_.userId === userId)
      .filter(_.jti === jti)
      .filter(_.source === source)
      .filter(_.adminId === adminId.orNull)
      .result
      .headOption
      .pipe(run)

  private def recordAccess(record: Record): Future[Boolean] = {
    val now = UtcTime.now
    val updateFrequencyStep = Duration.ofMinutes(5)
    val difference = Duration.between(record.updatedAt, now)

    if (difference.compareTo(updateFrequencyStep) == 1)
      updateUpdatedAt(record.id, now)
    else
      Future.successful(true)
  }

  def updateUpdatedAt(id: UUID, value: ZonedDateTime) =
    table
      .withFilter(_.id === id)
      .map(_.updatedAt)
      .update(value)
      .map(_ > 0)
      .pipe(runWithTransaction)

  private def findSessionRecord(token: JsonWebToken): Future[Option[Record]] =
    (for {
      userId <- getUserId(token)
      jti <- getJti(token)
      source <- getLoginSource(token)
    } yield (userId, jti, source)) match {
      case Some((userId, jti, source)) =>
        findOneMatchingTokenData(userId, jti, source, getAdminId(token)).map {
          _.map { matchingSession =>
            recordAccess(matchingSession)
            matchingSession
          }
        }

      case _ =>
        logger.error(s"Could not convert token into session: $token")

        Future.successful(None)
    }

  def queryDeleteByUserIds(userIds: Seq[UUID]) =
    table
      .filter(_.userId in userDao.deletableTable.map(_.id))
      .filter(_.userId inSet userIds)
      .delete

  def findByUserId(userId: UUID): Future[Seq[Record]] =
    table
      .filter(_.userId === userId)
      .result
      .pipe(run)

  def deleteByMerchantId(merchantId: UUID) =
    table
      .filter(_.userId in userDao.table.filter(_.merchantId === merchantId).map(_.id))
      .delete
      .pipe(run)

  def deleteByUserId(userId: UUID) =
    table
      .filter(_.userId === userId)
      .delete
      .pipe(run)

  def findOneByJti(jti: String) =
    table
      .filter(_.jti === jti)
      .result
      .headOption
      .pipe(run)

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId, f.userId, f.oauthAppName)
      .sortBy(_.createdAt.asc)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    queryFindAllByMerchantId(merchantId, f.userId)
      .length
      .result
      .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      userId: UUID,
      oauthAppName: Option[String] = None,
    ) =
    table
      .filter(_.userId === userId)
      .filter(t =>
        all(
          oauthAppName.map(appName =>
            t.id in oauthAppDao
              .queryFindByName(appName)
              .join(oauthAppSessionDao.table)
              .on(_.id === _.oauthAppId)
              .map(_._2.sessionId),
          ),
        ),
      )

  def deleteByIdsAndUserId(ids: Seq[UUID], userId: UUID): Future[Seq[UUID]] =
    if (ids.isEmpty)
      Future.successful(Seq.empty)
    else
      runWithTransaction(queryDeleteByIdsAndUserId(ids, userId))

  def queryDeleteByIdsAndUserId(ids: Seq[UUID], userId: UUID): DBIOAction[Seq[UUID], NoStream, Effect.Write] =
    table
      .filter(_.userId === userId)
      .filter(_.id inSet ids)
      .delete
      .map(_ => ids)
}
