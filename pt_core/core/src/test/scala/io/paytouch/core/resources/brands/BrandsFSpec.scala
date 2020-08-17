package io.paytouch.core.resources.brands

import java.util.UUID

import io.paytouch.core.data.model.BrandRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class BrandsFSpec extends FSpec {

  abstract class BrandResourceFSpecContext extends FSpecContext with DefaultFixtures {
    val brandDao = daos.brandDao

    def assertResponse(entity: Brand, record: BrandRecord) = {
      entity.id ==== record.id
      entity.name ==== record.name
    }

    def assertCreation(brandId: UUID, creation: BrandCreation) =
      assertUpdate(brandId, creation.asUpdate)

    def assertUpdate(brandId: UUID, update: BrandUpdate) = {
      val brand = brandDao.findById(brandId).await.get
      if (update.name.isDefined) update.name ==== Some(brand.name)
    }
  }
}
