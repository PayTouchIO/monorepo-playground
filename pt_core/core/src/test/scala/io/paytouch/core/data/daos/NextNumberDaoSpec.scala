package io.paytouch.core.data.daos

import io.paytouch.core.data.model.enums.{ NextNumberType, ScopeType }
import io.paytouch.core.entities.Scope
import io.paytouch.core.utils.{ DefaultFixtures, MockedRestApi, FixtureDaoFactory => Factory }

class NextNumberDaoSpec extends DaoSpec {

  abstract class NextNumberDaoSpecContext extends DaoSpecContext with DefaultFixtures {
    val dao: NextNumberDao = MockedRestApi.daos.nextNumberDao
    val locationScope = Scope.fromLocationId(london.id)
    val locationDailyScope =
      Scope.buildScope(ScopeType.LocationDaily, london.id.toString, Scope.dailyScopeKey(london.timezone))

    val numberType = NextNumberType.Order
    dao.run(dao.queryInsertNewNextNumber(locationScope, numberType, 10)).await
    dao.run(dao.queryInsertNewNextNumber(locationDailyScope, numberType, 20)).await
  }

  "NextNumberDao" in {
    "queryNextOrderNumberForLocationId" should {
      "if location settings configuration is not defined" in new NextNumberDaoSpecContext {
        dao.run(dao.queryNextOrderNumberForLocationId(london.id)).await
        dao.findByScopeAndType(locationScope, numberType).await.get.nextVal ==== 11
        dao.findByScopeAndType(locationDailyScope, numberType).await.get.nextVal ==== 20
      }

      "if location settings configuration is defined and set to Scope set Location" in new NextNumberDaoSpecContext {
        Factory.locationSettings(london, nextOrderNumberScopeType = Some(ScopeType.Location)).create

        dao.run(dao.queryNextOrderNumberForLocationId(london.id)).await
        dao.findByScopeAndType(locationScope, numberType).await.get.nextVal ==== 11
        dao.findByScopeAndType(locationDailyScope, numberType).await.get.nextVal ==== 20
      }

      "if location settings configuration is defined and set to Scope set LocationDaily" in new NextNumberDaoSpecContext {
        Factory.locationSettings(london, nextOrderNumberScopeType = Some(ScopeType.LocationDaily)).create

        dao.run(dao.queryNextOrderNumberForLocationId(london.id)).await
        dao.findByScopeAndType(locationScope, numberType).await.get.nextVal ==== 10
        dao.findByScopeAndType(locationDailyScope, numberType).await.get.nextVal ==== 21
      }
    }
  }
}
