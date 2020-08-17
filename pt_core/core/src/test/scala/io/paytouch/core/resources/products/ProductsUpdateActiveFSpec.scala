package io.paytouch.core.resources.products

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ProductsUpdateActiveFSpec extends GenericItemUpdateActiveFSpec[ArticleRecord, ProductLocationRecord] {

  lazy val productLocationDao = daos.productLocationDao
  lazy val articleDao = daos.articleDao

  def finder(id: UUID) = productLocationDao.findById(id)

  def itemFinder(id: UUID) = articleDao.findById(id)

  def namespace = "products"

  def singular = "product"

  def itemFactory(merchant: MerchantRecord) = Factory.templateProduct(merchant).create

  def itemLocationFactory(
      merchant: MerchantRecord,
      item: ArticleRecord,
      location: LocationRecord,
      active: Option[Boolean],
    ) =
    Factory.productLocation(item, location, active = active).create
}
