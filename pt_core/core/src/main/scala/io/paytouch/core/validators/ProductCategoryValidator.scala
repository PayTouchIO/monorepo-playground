package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.{ EntityOrdering, UserContext }
import io.paytouch.core.utils.Multiple

import scala.concurrent.ExecutionContext

class ProductCategoryValidator(
    val ptOrderingClient: PtOrderingClient,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) {

  val categoryValidator = new CategoryValidator(ptOrderingClient)
  val systemCategoryValidator = new SystemCategoryValidator(ptOrderingClient)
  val articleValidator = new MainArticleValidator

  def validateProductCategoryOrdering(categoryId: UUID, ordering: Seq[EntityOrdering])(implicit user: UserContext) =
    for {
      category <- categoryValidator.validateOneById(categoryId)
      products <- articleValidator.accessByIds(ordering.map(_.id))
    } yield Multiple.combine(category, products) { case _ => () }
}
