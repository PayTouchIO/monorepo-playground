package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities.Merchant
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.graphql.schema.Fetchers
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, ReplaceField, TransformFieldNames }
import sangria.schema.{ DeferredValue, Field }

trait BundleOptionDataType extends StringHelper { self: UUIDDataType with ProductDataType =>

  implicit private lazy val uuidT = UUIDType
  implicit private lazy val productT = ProductType

  lazy val BundleOptionDataType =
    deriveObjectType[GraphQLContext, BundleOption](
      TransformFieldNames(_.underscore),
      ReplaceField(
        "article",
        Field(
          "article",
          ProductType,
          resolve = { ctx =>
            implicit val ec = ctx.ctx.ec
            val productId = ctx.value.article.id.taggedWith[Product]
            val merchantId = ctx.ctx.merchantId.taggedWith[Merchant]
            val relId = (productId, merchantId)
            val productByProductId = Fetchers.products.defer(relId)
            DeferredValue(productByProductId).map { case (_, product) => product }
          },
        ),
      ),
    )

}
