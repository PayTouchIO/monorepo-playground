package io.paytouch.core.clients.urbanairship

import java.time.ZonedDateTime

import akka.http.scaladsl.model.{ HttpMethod, HttpMethods, HttpRequest }
import io.paytouch.core.{ UrbanAirshipHost, UrbanAirshipPassword, UrbanAirshipUsername }
import io.paytouch.core.clients.urbanairship.entities._
import io.paytouch.core.clients.urbanairship.entities.enums.ProjectType
import io.paytouch.core.utils.FixturesSupport
import io.paytouch.utils.Tagging._

class WalletClientSpec extends UAClientSpec {

  abstract class WalletClientSpec
      extends WalletClient(
        host.taggedWith[UrbanAirshipHost],
        user.taggedWith[UrbanAirshipUsername],
        pass.taggedWith[UrbanAirshipPassword],
      )
         with UAClientSpecContext
         with Fixtures
         with FixturesSupport {
    def assertRequest(
        request: HttpRequest,
        method: HttpMethod,
        path: String,
        jsonPostFile: Option[String] = None,
      ) = {
      request.method ==== method
      request.uri.path.toString ==== path
      request.headers.exists(h => h.name == "Authorization" && h.value == "Basic dXNlcjpwYXNz") should beTrue
      request.headers.exists(h => h.name == "Api-Revision" && h.value == "1.2") should beTrue
      if (jsonPostFile.isDefined)
        parse(request.entity.body) ==== parse(loadJson(s"/urbanairship/requests/${jsonPostFile.get}"))
    }
  }

  "WalletClient" should {

    "create a pass" in new WalletClientSpec {
      when(createPass(templateId, externalId, passUpsertion)).expectRequest { request =>
        assertRequest(request, HttpMethods.POST, s"/v1/pass/$templateId/id/$externalId", Some("pass_upsertion.json"))
      }
    }

    "update a pass" in new WalletClientSpec {
      val response = when(updatePass(externalId, passUpsertion))
        .expectRequest { request =>
          assertRequest(request, HttpMethods.PUT, s"/v1/pass/id/$externalId", Some("pass_upsertion.json"))
        }
        .respondWith("/urbanairship/responses/pass_update.json")
      response.await ==== Right(passUpdateResponse)
    }
  }

  trait Fixtures {
    val projectId = "12345"
    val templateId = "4567"
    val externalId = "my-external-id"

    val projectCreation = {
      val settingsCreation = ProjectSettingsCreation(
        barcode_alt_text = "123json=456789",
        barcode_label = "Member ID",
        barcode_default_value = "123456789",
        barcode_encoding = "iso-8859-1",
        barcode_type = "pdf417",
      )
      ProjectCreation(
        name = "Aztec Barcode",
        projectType = ProjectType.Loyalty,
        description = "Aztec Barcode",
        settings = Some(settingsCreation),
      )
    }

    val settings = ProjectSettings(
      id = None,
      barcodeAltText = "123json=456789",
      barcodeDefaultValue = "123456789",
      barcodeEncoding = "iso-8859-1",
      barcodeLabel = "Member ID",
      barcodeType = "pdf417",
      passbookCount = Some("2"),
      googleCount = Some("1"),
    )

    val settingsWithExternalId = settings.copy(id = Some(externalId))

    val project =
      Project(
        id = projectId,
        projectType = ProjectType.Loyalty,
        name = "Aztec Barcode",
        description = "Aztec Barcode",
        templates = Seq.empty,
        settings = settings,
        createdAt = ZonedDateTime.parse("2013-07-01T19:57:36.190Z"),
        updatedAt = ZonedDateTime.parse("2013-07-01T19:57:36.190Z"),
      )

    val projectWithExternalId = project.copy(settings = settingsWithExternalId)

    val projects = Seq(project)

    val passUpsertion = PassUpsertion(
      headers = Map(
        "barcode_value" -> FieldValueUpdate("abc1234567890"),
      ),
      fields = Map(
        "Member Name" -> FieldValueUpdate("Barak Obama"),
        "Points" -> FieldValueUpdate(57),
        "Merchant Website" -> FieldValueUpdate("https://www.whitehouse.gov"),
      ),
    )

    val passUpdateResponse = PassUpdateResponse(ticketId = "49993585")
    val pass = loadJsonAs[Pass](s"/urbanairship/responses/pass_creation.json")
  }

}
