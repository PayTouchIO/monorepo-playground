package io.paytouch.core.resources.users

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ User => UserEntity, _ }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class UsersListFSpec extends UsersFSpec {

  abstract class UsersListFSpecContext extends UserResourceFSpecContext

  "GET /v1/users.list" in {
    "if request has valid token" in {

      "with no parameters" should {
        "return a paginated list of all accessible users sorted by title" in new UsersListFSpecContext {
          val newYork = Factory.location(merchant).create
          val userNewYork = Factory.user(merchant, locations = Seq(newYork)).create
          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now), locations = locations).create

          Get("/v1/users.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val users = responseAs[PaginatedApiResponse[Seq[UserEntity]]].data
            users.map(_.id) ==== Seq(user.id)
            assertResponse(users.find(_.id == user.id).get, user, withPermissions = false)
          }
        }
      }

      "with location_id filters" should {
        "return a list filtered by location id" in new UsersListFSpecContext {
          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now)).create

          Get(s"/v1/users.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val users = responseAs[PaginatedApiResponse[Seq[UserEntity]]].data
            users.map(_.id) ==== Seq(user.id)
            assertResponse(users.find(_.id == user.id).get, user, withPermissions = false)
          }
        }
      }

      "with user_role_id filters" should {
        "return a list of users matching query" in new UsersListFSpecContext {
          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now), locations = locations).create
          val daniela = Factory.user(merchant, userRole = Some(userRole), locations = locations).create

          Get(s"/v1/users.list?user_role_id=${userRole.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val users = responseAs[PaginatedApiResponse[Seq[UserEntity]]].data
            users.map(_.id) should containTheSameElementsAs(Seq(daniela.id, user.id))
            assertResponse(users.find(_.id == daniela.id).get, daniela, withPermissions = false)
            assertResponse(users.find(_.id == user.id).get, user, withPermissions = false)
          }
        }
      }

      "with q filters" should {
        "return a list of users matching query" in new UsersListFSpecContext {
          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now), locations = locations).create
          val daniela = Factory.user(merchant, firstName = Some("Daniela"), locations = locations).create

          Get("/v1/users.list?q=dan").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val users = responseAs[PaginatedApiResponse[Seq[UserEntity]]].data
            users.map(_.id) ==== Seq(daniela.id)
            assertResponse(users.find(_.id == daniela.id).get, daniela, withPermissions = false)
          }
        }
      }

      "with updated_since filters" should {
        "return a list of users filtered by updated_since" in new UsersListFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now), locations = locations).create
          val daniela = Factory
            .user(merchant, firstName = Some("Daniela"), locations = locations, overrideNow = Some(now.plusDays(1)))
            .create
          val francesco = Factory
            .user(merchant, firstName = Some("Francesco"), locations = locations, overrideNow = Some(now.minusDays(1)))
            .create

          Get("/v1/users.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val users = responseAs[PaginatedApiResponse[Seq[UserEntity]]].data
            users.map(_.id) should containTheSameElementsAs(Seq(daniela.id, user.id))
            assertResponse(users.find(_.id == daniela.id).get, daniela, withPermissions = false)
            assertResponse(users.find(_.id == user.id).get, user, withPermissions = false)
          }
        }
      }

      "with expand[]=access" should {
        "return a paginated list of all accessible users sorted by title" in new UsersListFSpecContext {
          val newYork = Factory.location(merchant).create
          val userNewYork = Factory.user(merchant, locations = Seq(newYork)).create
          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now), locations = locations).create

          Get("/v1/users.list?expand[]=access").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val users = responseAs[PaginatedApiResponse[Seq[UserEntity]]].data
            users.map(_.id) ==== Seq(user.id)

            assertResponse(
              users.find(_.id == user.id).get,
              user,
              withPermissions = false,
              access = Some(buildAccess(userRole)),
            )
          }
        }
      }
    }
  }
}
