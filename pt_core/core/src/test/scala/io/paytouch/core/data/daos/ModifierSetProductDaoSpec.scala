package io.paytouch.core.data.daos

import io.paytouch.core.utils.{ MultipleLocationFixtures, UtcTime, FixtureDaoFactory => Factory }

class ModifierSetProductDaoSpec extends DaoSpec {
  lazy val modifierSetProductDao = daos.modifierSetProductDao

  abstract class ModifierSetProductDaoSpecContext extends DaoSpecContext with MultipleLocationFixtures

  "ModifierSetProductDao" in {
    "countByModifierSetIds" should {
      "don't count deleted products" in new ModifierSetProductDaoSpecContext {
        val product1 = Factory.simpleProduct(merchant).create
        val product2 = Factory.simpleProduct(merchant, deletedAt = Some(UtcTime.now)).create

        val modifierSet = Factory.modifierSet(merchant).create
        Factory.modifierSetProduct(modifierSet, product1).create
        Factory.modifierSetProduct(modifierSet, product2).create

        val expectedCount = Map(modifierSet.id -> 1)
        val resultCount = modifierSetProductDao.countByModifierSetIds(Seq(modifierSet.id)).await

        resultCount ==== expectedCount
      }
    }
  }

}
