package io.paytouch.core.data.daos

import io.paytouch.core.utils.{ MultipleLocationFixtures, UtcTime, FixtureDaoFactory => Factory }

class UserLocationDaoSpec extends DaoSpec {
  lazy val userLocationDao = daos.userLocationDao

  abstract class UserLocationDaoSpecContext extends DaoSpecContext with MultipleLocationFixtures

  "UserLocationDao" in {
    "findByItemIds" should {
      "filter user location by item ids and non deleted location ids" in new UserLocationDaoSpecContext {
        val deletedLocation = Factory.location(merchant, deletedAt = Some(UtcTime.now)).create
        Factory.userLocation(user, deletedLocation).create

        val expectedLocationIds = locations.map(_.id)
        val resultLocationIds = userLocationDao.findByItemId(user.id).map(_.map(_.locationId)).await

        resultLocationIds should containTheSameElementsAs(expectedLocationIds)
      }
    }
  }

}
