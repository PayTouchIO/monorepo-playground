package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._

import scala.concurrent._

class CatalogCategoryDao(
    val catalogDao: CatalogDao,
    val categoryAvailabilityDao: CategoryAvailabilityDao,
    val categoryLocationAvailabilityDao: CategoryLocationAvailabilityDao,
    anonymCategoryLocationDao: => CategoryLocationDao,
    val imageUploadDao: ImageUploadDao,
    anonymProductCategoryDao: => ProductCategoryDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends GenericCategoryDao {

  def categoryLocationDao = anonymCategoryLocationDao
  def productCategoryDao = anonymProductCategoryDao

  val withCatalogId = Some(true)
}
