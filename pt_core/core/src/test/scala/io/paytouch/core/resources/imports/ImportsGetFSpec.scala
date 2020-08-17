package io.paytouch.core.resources.imports

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.ImportRecord
import io.paytouch.core.entities.{ Import => ImportEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

class ImportsGetFSpec extends FSpec {

  abstract class ImportResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val importDao = daos.importDao
    val systemCategoryDao = daos.systemCategoryDao
    val categoryLocationDao = daos.categoryLocationDao
    val productDao = daos.productDao

    def assertResponse(record: ImportRecord, entity: ImportEntity) = {
      record.id ==== entity.id
      record.locationIds ==== entity.locationIds
      record.validationStatus ==== entity.validationStatus
      record.importStatus ==== entity.importStatus
      record.validationErrors ==== entity.validationErrors
      record.importSummary ==== entity.importSummary
      record.deleteExisting ==== entity.deleteExisting
      record.createdAt ==== entity.createdAt
      record.updatedAt ==== entity.updatedAt
    }
  }

  "GET /v1/imports.get?import_id=$" in {
    "if request has valid token" in {

      "if the import belongs to the merchant" should {
        "return a import" in new ImportResourceFSpecContext {
          val location = Factory.location(merchant).create
          val `import` = Factory.`import`(location).create

          Get(s"/v1/imports.get?import_id=${`import`.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val importResponse = responseAs[ApiResponse[ImportEntity]]
            assertResponse(`import`, importResponse.data)
          }
        }
      }

      "if the import does not belong to the merchant" should {
        "return 404" in new ImportResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorImport = Factory.`import`(competitorLocation).create

          Get(s"/v1/imports.get?import_id=${competitorImport.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
