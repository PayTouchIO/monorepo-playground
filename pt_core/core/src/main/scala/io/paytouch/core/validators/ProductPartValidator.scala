package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.{ ProductPartAssignment, UserContext }
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class ProductPartValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  val articleValidator = new ArticleValidator
  val partValidator = new PartValidator

  def validateProductPartAssignments(
      productId: UUID,
      assignments: Seq[ProductPartAssignment],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    for {
      validProduct <- articleValidator.accessOneById(productId)
      validParts <- partValidator.accessByIds(assignments.map(_.partId))
    } yield Multiple.combine(validProduct, validParts) { case _ => (): Unit }

}
