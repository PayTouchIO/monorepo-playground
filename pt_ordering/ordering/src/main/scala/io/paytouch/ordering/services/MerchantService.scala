package io.paytouch.ordering.services

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import cats.data.OptionT
import cats.implicits._
import io.paytouch.ordering.{ ServiceConfigurations, UpsertionResult }
import io.paytouch.ordering.conversions.MerchantConversions
import io.paytouch.ordering.data.daos.{ Daos, MerchantDao }
import io.paytouch.ordering.data.model
import io.paytouch.ordering.entities
import io.paytouch.ordering.errors.NotInDevelopment
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.services.features._
import io.paytouch.ordering.utils.StringHelper._
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.MerchantValidator

import scala.concurrent.{ ExecutionContext, Future }

class MerchantService(implicit val ec: ExecutionContext, val daos: Daos)
    extends MerchantConversions
       with FindByIdFeature
       with StandardUpdateFeature {
  type Context = entities.UserContext
  type Dao = MerchantDao
  type Entity = entities.Merchant
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Model = model.MerchantUpdate
  type Record = model.MerchantRecord
  type Validator = MerchantValidator
  type Upsertion = entities.MerchantUpdate

  val dao = daos.merchantDao
  protected val validator = new MerchantValidator

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()

  protected val expanders = Seq.empty

  def find(implicit context: entities.AppContext): Future[Option[Entity]] = {
    val id = context.merchantId
    val futOptT = for {
      record <- OptionT(dao.findById(id))
      entity <- OptionT.liftF(enrich(record))
    } yield entity
    futOptT.value
  }

  def update(upd: Update)(implicit user: entities.UserContext): Future[UpsertionResult[Entity]] =
    update(user.merchantId, upd)

  def findByStores(
      stores: Seq[model.StoreRecord],
    )(implicit
      context: entities.AppContext,
    ): Future[Map[model.StoreRecord, Entity]] =
    find.map { maybeEntity =>
      (for {
        store <- stores
        entity <- maybeEntity.toSeq
      } yield store -> entity).toMap
    }

  protected def convertToUpsertionModel(
      id: UUID,
      upsertion: Upsertion,
      existing: Option[Record],
    )(implicit
      user: entities.UserContext,
    ): Future[Model] =
    Future.successful(toUpsertionModel(id, upsertion))

  def validateUrlSlug(urlSlug: String)(implicit user: entities.UserContext): Future[ValidatedData[Unit]] =
    validator.validateUrlSlug(urlSlug)

  def findBySlug(slug: String): Future[Option[Entity]] =
    dao.findBySlug(slug).map(_.map(entities.Merchant.fromRecord))

  def findByIdWithoutContext(id: UUID): Future[Option[Entity]] =
    dao.findById(id).map(_.map(entities.Merchant.fromRecord))

  def getPaymentProcessorConfig(implicit context: entities.AppContext): Future[Option[model.PaymentProcessorConfig]] =
    dao.findPaymentProcessorConfig(context.merchantId)

  def generateUrlSlug(displayName: String): Future[String] = {
    val slug = displayName.urlsafe
    slug match {
      case "" =>
        // Display name has no valid characters
        val randomDigits = ThreadLocalRandom.current().nextInt(1000, 9999).toString()
        generateUrlSlug(randomSlugSuffix)
      case _ =>
        dao.existsUrlSlug(slug).flatMap {
          case true =>
            // Slug is already taken
            generateUrlSlug(slug + " " + randomSlugSuffix)

          case false =>
            // Slug is unique
            Future.successful(slug)
        }
    }
  }

  private def randomSlugSuffix: String =
    ThreadLocalRandom.current().nextInt(1000, 9999).toString()
}
