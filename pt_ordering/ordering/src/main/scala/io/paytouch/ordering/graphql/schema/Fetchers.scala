package io.paytouch.ordering.graphql.schema

import sangria.execution.deferred._
import io.paytouch.implicits._
import io.paytouch.ordering._
import io.paytouch.ordering.entities._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.services._

import scala.concurrent.Future

object Fetchers {
  lazy val categoryProducts = {
    val fetchOp = { (ctx: GraphQLContext, relIds: Seq[ProductService.RelId]) =>
      implicit val ec = ctx.ec

      ctx
        .services
        .productService
        .findAllPerRelId(
          ids = relIds,
          expansions = relIds.foldLeft(ProductExpansions.empty)(_ ++ _.expansions),
        )
        .map(_.toSeq)
    }

    Fetcher.caching(fetchOp) {
      HasId[(ProductService.RelId, Seq[Product]), ProductService.RelId] {
        case (id, _) => id
      }
    }
  }

  lazy val merchants = {
    type Id = CoreMerchantService#Id
    val fetchOp = { (ctx: GraphQLContext, ids: Seq[Id]) =>
      // fakes multiple fetch, when in fact we only run merchants.me once
      ids.headOption match {
        case Some(id) =>
          implicit val ec = ctx.ec
          ctx
            .services
            .coreMerchantService
            .findCoreEntityByMerchantId(id)
            .map(r => r.asOption.toSeq)
        case None => Future.successful(Seq.empty)
      }
    }
    Fetcher(fetchOp)(HasId(_.id.taggedWith[CoreMerchant]))
  }

  lazy val products = {
    type Id = (ProductService#Id)
    val fetchOp = { (ctx: GraphQLContext, ids: Seq[Id]) =>
      implicit val ec = ctx.ec
      ctx.services.productService.findAllPerId(ids).map(_.toSeq)
    }
    val config = FetcherConfig.maxBatchSize(20).caching
    Fetcher.caching(fetchOp, config)(HasId[(Id, Product), Id] { case (id, _) => id })
  }

  lazy val catalogs = {
    type Id = (CatalogService#Id)
    val fetchOp = { (ctx: GraphQLContext, ids: Seq[Id]) =>
      implicit val ec = ctx.ec
      ctx.services.catalogService.findAllPerId(ids).map(_.toSeq)
    }
    val config = FetcherConfig.maxBatchSize(20).caching
    Fetcher.caching(fetchOp, config)(HasId[(Id, Catalog), Id] { case (id, _) => id })
  }

  lazy val modifiers = {
    type Id = ModifierSetService#Id
    val fetchOp = { (ctx: GraphQLContext, ids: Seq[Id]) =>
      implicit val ec = ctx.ec
      ctx.services.modifierSetService.findAllPerId(ids)
    }
    val config = FetcherConfig.maxBatchSize(20).caching
    Fetcher(fetchOp, config)(HasId[(Id, ModifierSetService#Entity), Id] { case (id, _) => id })
  }

  lazy val storeLocations = {
    type RelId = LocationService#RelId
    val fetchOp = { (ctx: GraphQLContext, relIds: Seq[RelId]) =>
      implicit val ec = ctx.ec
      ctx.services.locationService.findPerRelId(relIds).map(_.toSeq)
    }

    Fetcher.caching(fetchOp)(HasId[(RelId, Option[Location]), RelId] {
      case (id, _) => id
    })
  }

  lazy val all =
    DeferredResolver
      .fetchers(
        categoryProducts,
        merchants,
        products,
        catalogs,
        storeLocations,
        modifiers,
      )
}
