package io.paytouch.core.data.daos

import cats.implicits._

import io.paytouch.core.data.model.{ Permission, PermissionUpdate, UserRoleUpdate }
import io.paytouch.core.entities.PermissionsUpdate
import io.paytouch.core.utils.{ MultipleLocationFixtures, UtcTime, FixtureDaoFactory => Factory }

class UserDaoSpec extends DaoSpec {
  lazy val userDao = daos.userDao

  abstract class UserDaoSpecContext extends DaoSpecContext with MultipleLocationFixtures

  "UserDao" in {
    "findUsersWithPermission" should {
      "filter user by item ids and non deleted ids" in new UserDaoSpecContext {
        val deleted =
          Factory
            .user(merchant, deletedAt = Some(UtcTime.now))
            .create

        val adminUserRole =
          userRole

        val admin =
          user

        val managerUserRole =
          Factory
            .userRole(
              merchant,
              name = Some("Manager"),
              permissions = Some(PermissionsUpdate.Paytouch.Manager),
            )
            .create

        val manager =
          Factory
            .user(merchant, userRole = Some(managerUserRole))
            .create

        val employeeUserRole = Factory
          .userRole(
            merchant,
            name = Some("Employee"),
            permissions = PermissionsUpdate
              .Paytouch
              .Employee
              .copy(cashDrawers = Some(PermissionUpdate.ReadAndEdit))
              .some,
          )
          .create

        val employee = Factory
          .user(merchant, userRole = Some(employeeUserRole))
          .create

        val cashierUserRole =
          Factory
            .userRole(
              merchant,
              name = Some("Cashier"),
              permissions = PermissionsUpdate
                .Paytouch
                .Cashier
                .copy(cashDrawers = Some(PermissionUpdate.ReadAndEdit))
                .some,
            )
            .create

        val cashier =
          Factory
            .user(
              merchant,
              userRole = Some(cashierUserRole),
            )
            .create

        val targettableButInactive =
          Factory
            .user(
              merchant,
              userRole = Some(managerUserRole),
              active = Some(false),
            )
            .create

        val expectedIds =
          Seq(admin, manager).map(_.id)

        val resultIds =
          userDao
            .findUsersWithPermission(
              merchant.id,
              "register",
              "cashDrawers",
              Permission(create = true),
            )
            .map(_.map(_.id))
            .await

        resultIds must containTheSameElementsAs(expectedIds)
      }
    }
  }
}
