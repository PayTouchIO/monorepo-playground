package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.DaoSpec
import io.paytouch.core.entities.ArticleUpdate
import io.paytouch.core.utils.{ DefaultFixtures, FixtureDaoFactory => Factory }
import io.paytouch.utils.TestExecutionContext
import org.specs2.specification.Scope

class ArticleValidatorSpec extends DaoSpec {

  abstract class ArticleValidatorSpecContext extends TestExecutionContext with Scope with DefaultFixtures {
    val newProductId = UUID.randomUUID

    lazy val articleValidator = new ArticleValidator
  }

  "ArticleValidator" in {
    "validateUpsertion" in {
      "with an empty update" should {
        "return Valid" in new ArticleValidatorSpecContext {
          val update = random[ArticleUpdate].copy(sku = "")
          val validation = articleValidator.validateUpsertion(newProductId, update)(userContext).await
          validation.success ==== update
        }
      }

      "with an existing product with null sku and update with null sku" should {
        "return Valid" in new ArticleValidatorSpecContext {
          val existingProductNullSku = Factory.simpleProduct(merchant, sku = None).create
          val update = random[ArticleUpdate].copy(sku = None)
          val validation = articleValidator.validateUpsertion(newProductId, update)(userContext).await
          validation.success ==== update
        }
      }
    }
  }

}
