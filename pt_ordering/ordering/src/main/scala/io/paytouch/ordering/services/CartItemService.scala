package io.paytouch.ordering.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.ordering.{ Result, UpsertionResult }
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.conversions.CartItemConversions
import io.paytouch.ordering.data.daos.{ CartItemDao, Daos }
import io.paytouch.ordering.data.model.{ CartItemRecord, CartRecord }
import io.paytouch.ordering.data.model.upsertions.{ CartItemUpsertion => CartItemUpsertionModel }
import io.paytouch.ordering.entities.{ CartItem => CartItemEntity, CartItemUpsertion => CartItemUpsertionEntity, _ }
import io.paytouch.ordering.entities.CreationEntity
import io.paytouch.ordering.expansions.NoExpansions
import io.paytouch.ordering.filters.NoFilters
import io.paytouch.ordering.services.features._
import io.paytouch.ordering.validators.CartItemValidator

class CartItemService(
    val ptCoreClient: PtCoreClient,
    val cartItemModifierOptionService: CartItemModifierOptionService,
    val cartItemTaxRateService: CartItemTaxRateService,
    val cartItemVariantOptionService: CartItemVariantOptionService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends CartItemConversions
       with FindExpansionByRecordFeature
       with DefaultDeleteStoreFeature {
  type Creation = CartItemCreation
  type Dao = CartItemDao
  type Entity = CartItemEntity
  type Expansions = NoExpansions
  type Filters = NoFilters
  type Model = CartItemUpsertionModel
  type Record = CartItemRecord
  type Update = CartItemUpdate
  type Upsertion = CartItemUpsertionEntity
  type Validator = CartItemValidator

  protected val dao = daos.cartItemDao
  protected val validator = new CartItemValidator(ptCoreClient)

  protected val defaultFilters = NoFilters()
  protected val defaultExpansions = NoExpansions()

  protected val expanders = {
    val cartItemModifierOptionExpander = MandatoryExpander[this.type, Seq[CartItemModifierOption]](
      retrieverF = implicit context => cartItemModifierOptionService.findItemModifierOptionsByCart,
      copierF = (entity, data) => entity.copy(modifierOptions = data),
      defaultDataValue = Seq.empty,
    )

    val cartItemTaxRateExpander = MandatoryExpander[this.type, Seq[CartItemTaxRate]](
      retrieverF = implicit context => cartItemTaxRateService.findItemTaxRatesByCart,
      copierF = (entity, data) => entity.copy(taxRates = data),
      defaultDataValue = Seq.empty,
    )

    val cartItemVariantOptionExpander = MandatoryExpander[this.type, Seq[CartItemVariantOption]](
      retrieverF = implicit context => cartItemVariantOptionService.findItemVariantOptionsByCart,
      copierF = (entity, data) => entity.copy(variantOptions = data),
      defaultDataValue = Seq.empty,
    )

    Seq(cartItemModifierOptionExpander, cartItemTaxRateExpander, cartItemVariantOptionExpander)
  }

  def findItemsByCart(
      cartRecords: Seq[CartRecord],
    )(implicit
      context: AppContext,
    ): Future[Map[CartRecord, Seq[Entity]]] =
    findEntitiesByRecord(cartRecords, dao.findByCartIds, _.cartId)

  def createOrMerge(
      cart: Cart,
      creation: CartItemCreation,
    )(implicit
      context: Context,
    ): Future[UpsertionResult[Entity]] =
    validator
      .validateUpsertion(creation.productId, creation.asUpsert)
      .flatMapValid { validUpsertion =>
        upsertItem(
          findMergeable(cart, creation)
            .fold(toUpsertionModel(UUID.randomUUID, validUpsertion)) { item =>
              toUpsertionModel(
                id = item.id,
                upsertion = validUpsertion
                  .copy(
                    upsertion = validUpsertion
                      .upsertion
                      .copy(
                        modifierOptions = None,
                        quantity = validUpsertion
                          .upsertion
                          .quantity
                          .map(_ + item.quantity),
                      ),
                  ),
              )
            },
        )
      }

  def update(
      cart: Cart,
      cartItemId: UUID,
      update: Update,
    )(implicit
      context: Context,
    ): Future[UpsertionResult[Entity]] =
    validator.validateCartItemId(cart, cartItemId).flatMap {
      case Validated.Valid(cartItem) =>
        validator
          .validateUpsertion(cartItem.product.id, update.asUpsert)
          .flatMapValid(validUpsertion => upsertItem(toUpsertionModel(cartItem.id, validUpsertion)))

      case i @ Validated.Invalid(_) =>
        i.pure[Future]
    }

  protected def upsertItem(model: Model)(implicit context: Context): Future[Result[Entity]] =
    for {
      (resultType, record) <- dao.upsert(model)
      entity <- enrich(record)
    } yield resultType -> entity
}
