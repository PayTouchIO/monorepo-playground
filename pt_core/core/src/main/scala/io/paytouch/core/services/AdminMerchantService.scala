package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._

import io.paytouch.core._
import io.paytouch.core.BcryptRounds
import io.paytouch.core.conversions.MerchantConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.extensions.DynamicSortBySupport.Sortings
import io.paytouch.core.data.model.MerchantRecord
import io.paytouch.core.data.model.upsertions.MerchantUpsertion
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ContextSource
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.core.filters.MerchantFilters
import io.paytouch.core.utils._
import io.paytouch.core.utils.FindResult._
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.MerchantValidator

class AdminMerchantService(
    val authenticationService: AuthenticationService,
    val bcryptRounds: Int withTag BcryptRounds,
    val catalogService: CatalogService,
    val hmacService: HmacService,
    val locationService: LocationService,
    val locationReceiptService: LocationReceiptService,
    merchantService: => MerchantService,
    sampleDataService: => SampleDataService,
    val userRoleService: UserRoleService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends MerchantConversions {

  type Record = MerchantService#Record
  type Entity = MerchantService#Entity
  type Upsertion = MerchantUpsertion
  type Filters = MerchantFilters
  type Expansions = MerchantExpansions

  protected val dao = daos.merchantDao
  protected val validator = new MerchantValidator

  def create(id: UUID, creation: MerchantCreation): Future[ErrorsOr[Result[Entity]]] =
    validator.isIdAvailable(id).flatMap {
      case Valid(_)       => convertAndCreate(id, creation)
      case i @ Invalid(_) => Future.successful(i)
    }

  private def convertAndCreate(id: UUID, creation: MerchantCreation): Future[ErrorsOr[Result[Entity]]] =
    convertToUpsertionModel(id, creation).flatMapTraverse(createMerchantWithRelations(creation))

  def update(
      id: UUID,
      update: AdminMerchantUpdate,
    )(implicit
      admin: AdminContext,
    ): Future[ErrorsOr[Result[Entity]]] =
    validator.adminAccessById(id).flatMap {
      case Valid(_)       => convertAndUpdate(id, update)
      case i @ Invalid(_) => Future.successful(i)
    }

  private def convertAndUpdate(id: UUID, update: AdminMerchantUpdate): Future[ErrorsOr[Result[Entity]]] =
    convertToUpdateModel(id, update).flatMapTraverse { updateModel =>
      for {
        (resultType, merchantRecord) <- dao.upsert(updateModel)
        entities <- merchantService.enrich(Seq(merchantRecord))(merchantService.defaultExpansions)
      } yield (resultType, entities.head)
    }

  private def convertToUpdateModel(id: UUID, update: AdminMerchantUpdate): Future[ErrorsOr[model.MerchantUpdate]] =
    Multiple
      .success(fromAdminMerchantUpdateEntityToUpdateModel(id, update))
      .pure[Future]

  private def convertToUpsertionModel(merchantId: UUID, creation: MerchantCreation): Future[ErrorsOr[Upsertion]] =
    for {
      validMerchant <- convertToMerchantUpdate(merchantId, creation)
      validUserRoles <- userRoleService.convertToDefaultUserRoleUpdates(merchantId, creation.setupType)
      validUser <- userService.convertToUserUpdate(merchantId, creation)
    } yield Multiple.combine(validMerchant, validUserRoles, validUser)(MerchantUpsertion)

  private def createMerchantWithRelations(
      merchantCreation: MerchantCreation,
    )(
      creationModel: Upsertion,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, merchant, userOwner) <- dao.create(creationModel)
      userContext = inferUserContext(merchant, userOwner, ContextSource.PtAdmin)
      _ <- locationService.createDefaultLocation(merchant, userOwner, merchantCreation)(userContext)
      _ <- catalogService.createDefaultMenu(userContext)
      _ = triggerBackgroundOps(merchant)(userContext)
    } yield (resultType, fromMerchantAndUserRecordToEntity(merchant, userOwner))

  private def triggerBackgroundOps(merchant: MerchantRecord)(implicit user: UserContext) = {
    sampleDataService.load(merchant)
    merchantService.sendMerchantChangedMessage(merchant)
  }

  private def convertToMerchantUpdate(
      id: UUID,
      creation: MerchantCreation,
    ): Future[ErrorsOr[model.MerchantUpdate]] =
    Multiple
      .success(fromMerchantCreationToUpdate(id, creation))
      .pure[Future]

  def findAll(
      filters: Filters,
    )(
      expansions: Expansions,
      sortings: Sortings,
    )(implicit
      admin: AdminContext,
      pagination: Pagination,
    ): Future[FindResult[Entity]] = {
    val itemsResp = dao.findAllWithFilters(filters)(sortings)(pagination.offset, pagination.limit)
    val countResp = dao.countAllWithFilters(filters)
    for {
      items <- itemsResp
      enrichedData <- enrich(items)(expansions)
      count <- countResp
    } yield (enrichedData, count)
  }

  def findById(merchantId: UUID)(expansions: Expansions)(implicit admin: AdminContext): Future[Option[Entity]] =
    dao.findById(merchantId).flatMap(result => enrich(result.toSeq)(expansions).map(_.headOption))

  def enrich(merchants: Seq[Record])(e: Expansions)(implicit admin: AdminContext): Future[Seq[Entity]] =
    merchantService.enrich(merchants)(e)
}
