package io.paytouch.ordering.graphql.categories

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day
import io.paytouch.ordering.clients.paytouch.core.entities.{ Availability, Category, CategoryLocation, ImageUrls }
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.graphql.SchemaSpec
import io.paytouch.ordering.utils.{ CommonArbitraries, DefaultFixtures }
import org.scalacheck.Arbitrary

abstract class CategoriesSchemaSpec extends SchemaSpec with CommonArbitraries {

  abstract class CategoriesSchemaSpecContext extends SchemaSpecContext with DefaultFixtures {
    lazy val expectedSubcategories = Seq.empty[Category]
    lazy val expectedImageUrls = Seq.empty[ImageUrls]
    lazy val expectedLocationOverrides = Map.empty[UUID, CategoryLocation]
    lazy val expectedAvailabilities = Map.empty[Day, Seq[Availability]]

    @scala.annotation.nowarn("msg=Auto-application")
    lazy val expectedCategory =
      random[Category]
        .copy(
          catalogId = londonCatalogId,
          subcategories = expectedSubcategories,
          avatarImageUrls = expectedImageUrls,
          locationOverrides = Some(expectedLocationOverrides),
          availabilities = Some(expectedAvailabilities),
        )

    val store = londonStore
    val catalogId = londonCatalogId
    val locationId = store.locationId
    val storeContext = StoreContext.fromRecord(store)
    implicit val coreAuthToken = ptCoreClient.generateAuthHeaderForCore(storeContext)
  }
}
