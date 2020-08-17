package io.paytouch.ordering.graphql

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import sangria.schema._

import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.services._
import io.paytouch.ordering.services.features._
import io.paytouch.ordering.clients.paytouch.core.expansions._

final case class GraphQLContext(
    services: GraphQLService,
    merchantId: Option[UUID] = None,
    locationId: Option[UUID] = None,
  ) extends LowestPrioActions {
  implicit val ec = services.storeService.ec

  def findByIdWithContextUpdate[S <: FindByIdFromCoreFeature](service: S)(id: UUID) =
    withContextUpdate[Option[service.Entity]](
      storeContextF = service.getStoreContext(id),
      dataF = service.findById(id)(_),
      dataExtractorF = _.flatMap { case (_, data) => data },
    )

  def findAllWithContextUpdate[S <: FindAllFromCoreFeature](
      service: S,
    )(
      filters: service.Filters,
    ): MappedUpdateCtx[GraphQLContext, Option[(StoreContext, Seq[service.Entity])], Seq[service.Entity]] =
    withContextUpdate[Seq[service.Entity]](
      storeContextF = service.getStoreContext(filters),
      dataF = service.findAll(filters)(_),
      dataExtractorF = _.map { case (_, data) => data }.getOrElse(Seq.empty),
    )

  def withContextUpdate[T](
      storeContextF: => Future[Option[StoreContext]],
      dataF: StoreContext => Future[T],
      dataExtractorF: Option[(StoreContext, T)] => T,
    ): MappedUpdateCtx[GraphQLContext, Option[(StoreContext, T)], T] =
    UpdateCtx {
      val futureOpt = for {
        storeContext <- OptionT(storeContextF)
        data <- OptionT.liftF(dataF(storeContext))
      } yield (storeContext, data)
      futureOpt.value
    } { storeContextWithData =>
      val storeContext = storeContextWithData.map {
        case (context, _) => context
      }
      val optMerchantId = storeContext.map(_.merchantId)
      val optLocationId = storeContext.map(_.locationId)
      update(optMerchantId = optMerchantId, optLocationId = optLocationId)
    }.map(dataExtractorF)

  def update(optMerchantId: Option[UUID], optLocationId: Option[UUID]): GraphQLContext = {
    val updatedMerchantId = optMerchantId.orElse(merchantId)
    val updatedLocationId = optLocationId.orElse(locationId)
    copy(merchantId = updatedMerchantId, locationId = updatedLocationId)
  }
}

final case class GraphQLService(
    catalogService: CatalogService,
    categoryService: CategoryService,
    coreMerchantService: CoreMerchantService,
    giftCardService: GiftCardService,
    locationService: LocationService,
    merchantService: MerchantService,
    modifierSetService: ModifierSetService,
    orderService: OrderService,
    productService: ProductService,
    storeService: StoreService,
    tableService: TableService,
  )
