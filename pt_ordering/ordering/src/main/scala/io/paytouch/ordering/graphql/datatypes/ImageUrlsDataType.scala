package io.paytouch.ordering.graphql.datatypes

import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls
import io.paytouch.ordering.clients.paytouch.core.entities.enums.ImageSize
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper
import sangria.macros.derive.{ deriveObjectType, TransformFieldNames }
import sangria.schema._

trait ImageUrlsDataType extends StringHelper { self: EnumDataType with UUIDDataType =>
  implicit private lazy val imageSizeT = ImageSizeType
  implicit private lazy val uuidT = UUIDType

  implicit private lazy val urlsT = {
    val urlsFields: Seq[Field[GraphQLContext, Map[ImageSize, String]]] =
      ImageSize.values.map { imgSize =>
        Field(
          imgSize.entryName,
          OptionType(StringType),
          resolve = { ctx: Context[GraphQLContext, Map[ImageSize, String]] =>
            ctx.value.get(imgSize)
          },
        )
      }
    ObjectType("Urls", fields(urlsFields: _*))
  }

  lazy val ImageUrlsType = deriveObjectType[GraphQLContext, ImageUrls](TransformFieldNames(_.underscore))
}
