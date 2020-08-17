package io.paytouch.ordering.services

import java.util.UUID

import akka.http.scaladsl.model.headers.Authorization
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering._
import io.paytouch.implicits._
import io.paytouch.ordering.clients.CoreApiResponse
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.expansions._
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.entities.{ ApiResponse, Merchant }
import io.paytouch.ordering.services.features.FindAllByMerchantIdFromCoreFeature

import scala.concurrent.{ ExecutionContext, Future }

class ModifierSetService(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends LazyLogging {
  type Entity = ModifierSet
  type Id = (UUID withTag ModifierSet, Option[UUID] withTag Merchant)

  def findAllPerId(ids: Seq[Id]): Future[Seq[(Id, Entity)]] = {
    val modifierSetIds = ids.map(_._1)
    val maybeMerchantId = ids.flatMap(_._2).headOption
    if (modifierSetIds.nonEmpty && maybeMerchantId.nonEmpty) {
      val merchantId = maybeMerchantId.get
      implicit val token = ptCoreClient.generateAuthHeaderForCoreMerchant(merchantId)
      ptCoreClient.modifierSetsListByIds(modifierSetIds).map {
        case Right(ApiResponse(data, _)) =>
          data.map(entity => (entity.id.taggedWith[Entity], Some(merchantId).taggedWith[Merchant]) -> entity)
        case Left(error) =>
          val className = this.getClass.getSimpleName
          val errorMsg =
            s"""Error while performing findAll for $className
               |(params: ids[]=$modifierSetIds and merchant $merchantId).
               |Returning empty sequence. [${error.uri} -> ${error.errors}]""".stripMargin
          logger.error(errorMsg)
          Seq.empty
      }
    }
    else {
      val description = s"modifierSetIds -> $modifierSetIds; merchantId -> $maybeMerchantId"
      val errorMsg =
        s"""Data missing from GraphQLContext!
           |Expected to find at least a modifierSetId and merchantId.
           |Found: $description""".stripMargin
      logger.error(errorMsg)
      Future.successful(Seq.empty)
    }
  }
}
