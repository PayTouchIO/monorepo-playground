package io.paytouch.core.resources.validators

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ DefaultFixtures, FSpec, FixtureDaoFactory => Factory }

class IdsValidateFSpec extends FSpec {

  abstract class IdsValidateFSpecContext extends FSpecContext with DefaultFixtures {
    val competitor = Factory.merchant.create

    val imageUpload = Factory.imageUpload(merchant).create
    val imageId = imageUpload.id
    val imageType = imageUpload.objectType

    val catalog = Factory.catalog(merchant).create

    val baseIdsToValidate = IdsToValidate(
      locationIds = Seq(london.id),
      catalogIds = Seq(catalog.id),
      imageUploadIds = Map(imageType -> Seq(imageId)),
    )
  }

  "POST /v1/ids.validate" in {
    "if request has valid token" in {

      "return ok if all the ids are valid" in new IdsValidateFSpecContext {
        val idsToValidate = baseIdsToValidate

        Post("/v1/ids.validate", idsToValidate).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
        }
      }

      "return error if any location id is invalid" in new IdsValidateFSpecContext {
        val competitorLocation = Factory.location(competitor).create

        val idsToValidate = baseIdsToValidate.copy(locationIds = Seq(competitorLocation.id))

        Post("/v1/ids.validate", idsToValidate).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }

      "return error if any catalog id is invalid" in new IdsValidateFSpecContext {
        val competitorCatalog = Factory.catalog(competitor).create

        val idsToValidate = baseIdsToValidate.copy(catalogIds = Seq(competitorCatalog.id))

        Post("/v1/ids.validate", idsToValidate).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }

      "return error if any image id is invalid" in new IdsValidateFSpecContext {
        val competitorImage = Factory.imageUpload(competitor).create

        val idsToValidate =
          baseIdsToValidate.copy(imageUploadIds = Map(competitorImage.objectType -> Seq(competitorImage.id)))

        Post("/v1/ids.validate", idsToValidate).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }

      "return error if any image type doesn't match" in new IdsValidateFSpecContext {
        val invalidImageType = ImageUploadType.values.filterNot(_ == imageType).head

        val idsToValidate =
          baseIdsToValidate.copy(imageUploadIds = Map(invalidImageType -> Seq(imageId)))

        Post("/v1/ids.validate", idsToValidate).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)

          assertErrorCode("InvalidImageUploadAssociation")
        }
      }

    }

    "if request has invalid token" should {

      "be rejected" in new IdsValidateFSpecContext {

        Post(s"/v1/ids.validate").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
