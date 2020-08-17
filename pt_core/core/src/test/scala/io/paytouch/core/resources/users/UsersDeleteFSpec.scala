package io.paytouch.core.resources.users

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class UsersDeleteFSpec extends FSpec {

  lazy val userDao = daos.userDao
  lazy val sessionDao = daos.sessionDao

  abstract class UserResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {

    def assertUserIsMarkedAsDeleted(id: UUID) = {
      val user = userDao.findDeletedById(id).await
      user should beSome
      user.flatMap(_.deletedAt) should beSome
      user.flatMap(_.pin) should beNone
    }

    def assertUserIsMarkedAsNotDeleted(id: UUID) = {
      val user = userDao.findDeletedById(id).await
      user should beSome
      user.flatMap(_.deletedAt) should beNone
    }

    def assertUserDoesntExist(id: UUID) = {
      userDao.findDeletedById(id).await should beNone
      userDao.findById(id).await should beNone
    }

    def assertUserExists(id: UUID) = userDao.findById(id).await should beSome
  }

  "POST /v1/users.delete" in {

    "if request has valid token" in {
      "if user doesn't exist" should {
        "do nothing and return 204" in new UserResourceFSpecContext {
          val nonExistingUserId = UUID.randomUUID

          Post(s"/v1/users.delete", Ids(ids = Seq(nonExistingUserId)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
            assertUserDoesntExist(nonExistingUserId)
          }
        }
      }

      "if user belongs to the merchant" should {
        "delete the non-owner user and return 204" in new UserResourceFSpecContext {
          val nonOwnerUser = Factory.user(merchant, isOwner = Some(false), pin = Some("some-pin")).create
          Factory.createValidTokenWithSession(nonOwnerUser)

          Post(s"/v1/users.delete", Ids(ids = Seq(nonOwnerUser.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertUserIsMarkedAsDeleted(nonOwnerUser.id)
            sessionDao.findByUserId(nonOwnerUser.id).await ==== Seq.empty

          }
        }

        "do not delete the owner user and return 204" in new UserResourceFSpecContext {
          val ownerUser = Factory.user(merchant, isOwner = Some(true), pin = Some("another-pin")).create
          Factory.createValidTokenWithSession(ownerUser)

          Post(s"/v1/users.delete", Ids(ids = Seq(ownerUser.id))).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertUserIsMarkedAsNotDeleted(ownerUser.id)
            sessionDao.findByUserId(ownerUser.id).await.nonEmpty should beTrue
          }
        }
      }

      "if user belongs to a different merchant" should {
        "do not delete the user and return 204" in new UserResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create

          Post(s"/v1/users.delete", Ids(ids = Seq(competitorUser.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            assertUserExists(competitorUser.id)
          }
        }
      }

    }

  }
}
