package io.paytouch.core.services

import java.util.UUID

import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._

import io.paytouch.core.{ withTag, BcryptRounds }
import io.paytouch.core.conversions.AdminConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ AdminRecord, AdminUpdate => AdminUpdateModel }
import io.paytouch.core.entities.{ Admin => AdminEntity, AdminUpdate => AdminUpdateEntity, _ }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.utils.FindResult._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.AdminValidator

import scala.concurrent._

class AdminService(val bcryptRounds: Int withTag BcryptRounds)(implicit val ec: ExecutionContext, val daos: Daos)
    extends AdminConversions {

  protected val dao = daos.adminDao
  protected val validator = new AdminValidator
  val defaultFilters = NoFilters()

  def findAdminInfoByEmail(email: String): Future[Option[AdminLogin]] =
    dao.findAdminLoginByEmail(email)

  def getAdminContext(adminId: UUID): Future[Option[AdminContext]] =
    dao.findById(adminId).map(admin => admin.map(a => AdminContext(id = a.id)))

  def findById(
      id: UUID,
      filters: NoFilters = defaultFilters,
    )(
      expansions: NoExpansions,
    )(implicit
      admin: AdminContext,
    ): Future[Option[AdminEntity]] =
    validator.accessOneById(id).flatMapTraverse(item => enrich(item, filters)(expansions)).map(_.toOption)

  def findAll(
      filters: NoFilters,
    )(
      expansions: NoExpansions,
    )(implicit
      admin: AdminContext,
      pagination: Pagination,
    ): Future[FindResult[AdminEntity]] = {
    val itemsResp = dao.findAll(pagination.offset, pagination.limit)
    val countResp = dao.countAll
    for {
      items <- itemsResp
      enrichedData <- enrich(items, filters)(expansions)
      count <- countResp
    } yield (enrichedData, count)
  }

  def enrich(
      item: AdminRecord,
      filters: NoFilters,
    )(
      expansions: NoExpansions,
    )(implicit
      admin: AdminContext,
    ): Future[AdminEntity] =
    enrich(Seq(item), filters)(expansions).map(_.head)

  def enrich(
      admins: Seq[AdminRecord],
      f: NoFilters,
    )(
      e: NoExpansions,
    )(implicit
      admin: AdminContext,
    ): Future[Seq[AdminEntity]] =
    Future.successful(fromRecordsToEntities(admins))

  def create(
      id: UUID,
      creation: AdminCreation,
    )(implicit
      admin: AdminContext,
    ): Future[ErrorsOr[Result[AdminEntity]]] =
    validator.availableOneById(id).flatMap {
      case Valid(_)       => convertAndUpsert(id, creation.asUpdate)
      case i @ Invalid(_) => Future.successful(i)
    }

  def update(
      id: UUID,
      update: AdminUpdateEntity,
    )(implicit
      admin: AdminContext,
    ): Future[ErrorsOr[Result[AdminEntity]]] =
    validator.accessOneById(id).flatMap {
      case Valid(_)       => convertAndUpsert(id, update)
      case i @ Invalid(_) => Future.successful(i)
    }

  protected def convertToUpsertionModel(
      id: UUID,
      update: AdminUpdateEntity,
    )(implicit
      admin: AdminContext,
    ): Future[ErrorsOr[AdminUpdateModel]] =
    validator.validateUpsertion(id, update).mapNested(_ => fromUpsertionToUpdate(id, update))

  def convertAndUpsert(
      id: UUID,
      update: AdminUpdateEntity,
    )(implicit
      admin: AdminContext,
    ): Future[ErrorsOr[Result[AdminEntity]]] =
    convertToUpsertionModel(id, update).flatMapTraverse { upsertionModel =>
      dao.upsert(upsertionModel).map {
        case (resultType, record) =>
          (resultType, fromRecordToEntity(record))
      }
    }
}
