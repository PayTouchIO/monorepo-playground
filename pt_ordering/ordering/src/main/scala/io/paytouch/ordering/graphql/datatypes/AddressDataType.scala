package io.paytouch.ordering.graphql.datatypes

import sangria.macros.derive._
import sangria.schema._

import io.paytouch._

import io.paytouch.ordering.clients.paytouch.core.{ entities => CoreEntities }
import io.paytouch.ordering.entities.Address
import io.paytouch.ordering.graphql._
import io.paytouch.ordering.graphql.GraphQLContext
import io.paytouch.ordering.utils.StringHelper

trait AddressDataType extends StringHelper {
  lazy val AddressType: ObjectType[GraphQLContext, Address] =
    deriveObjectType(TransformFieldNames(_.underscore))

  implicit lazy val CountryCodeType: ScalarAlias[CoreEntities.Country.Code, String] =
    OpaqueStringType(CoreEntities.Country.Code)

  implicit lazy val CountryNameType: ScalarAlias[CoreEntities.Country.Name, String] =
    OpaqueStringType(CoreEntities.Country.Name)

  implicit lazy val CountryType: ObjectType[GraphQLContext, CoreEntities.Country] =
    deriveObjectType(TransformFieldNames(_.underscore))

  implicit lazy val StateCodeType: ScalarAlias[CoreEntities.State.Code, String] =
    OpaqueStringType(CoreEntities.State.Code)

  implicit lazy val StateNameType: ScalarAlias[CoreEntities.State.Name, String] =
    OpaqueStringType(CoreEntities.State.Name)

  implicit lazy val StateType: ObjectType[GraphQLContext, CoreEntities.State] =
    deriveObjectType(TransformFieldNames(_.underscore))

  implicit lazy val CoreAddressType: ObjectType[GraphQLContext, CoreEntities.Address] =
    deriveObjectType(TransformFieldNames(_.underscore))
}
