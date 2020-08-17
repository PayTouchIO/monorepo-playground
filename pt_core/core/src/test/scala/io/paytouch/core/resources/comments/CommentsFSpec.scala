package io.paytouch.core.resources.comments

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.CommentType
import io.paytouch.core.entities.{ CommentUpdate, Comment => CommentEntity, _ }
import io.paytouch.core.utils.{ FSpec, MultipleLocationFixtures, FixtureDaoFactory => Factory }

final case class Resource[T <: SlickMerchantRecord](
    singular: String,
    commentType: CommentType,
    createItem: (MerchantRecord, LocationRecord, UserRecord) => T,
  ) {
  val namespace = s"${singular}s"
  val parameter = s"${singular}_id"
  val name = singular.replace("_", " ")
}

abstract class CommentsFSpec extends FSpec with CommentResponseAssertions {

  val supportedResources = Seq(
    Resource[InventoryCountRecord](
      "inventory_count",
      CommentType.InventoryCount,
      (merchant, location, user) => Factory.inventoryCount(location, user).create,
    ),
    Resource[PurchaseOrderRecord](
      "purchase_order",
      CommentType.PurchaseOrder,
      (merchant, location, user) => Factory.purchaseOrder(merchant, location, user).create,
    ),
    Resource[ReceivingOrderRecord](
      "receiving_order",
      CommentType.ReceivingOrder,
      (merchant, location, user) => Factory.receivingOrder(location, user).create,
    ),
    Resource[ReturnOrderRecord](
      "return_order",
      CommentType.ReturnOrder,
      { (merchant, location, user) =>
        val supplier = Factory.supplier(merchant).create
        Factory.returnOrder(user, supplier, location).create
      },
    ),
  )

  abstract class CommentsFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val commentDao = daos.commentDao

    def assertCommentCreation(commentId: UUID, creation: CommentCreation) =
      assertCommentUpdate(commentId, creation.asUpdate)

    def assertCommentUpdate(commentId: UUID, update: CommentUpdate) = {
      val comment = commentDao.findById(commentId).await.get
      if (update.objectId.isDefined) update.objectId ==== Some(comment.objectId)
      if (update.body.isDefined) update.body ==== Some(comment.body)
    }

    def assertCommentResponse(
        entity: CommentEntity,
        record: CommentRecord,
        user: UserRecord,
      ) = {
      entity.id ==== record.id
      entity.user.id ==== user.id
      entity.body ==== record.body
      entity.createdAt ==== record.createdAt
    }

    def assertCommentResponseById(
        recordId: UUID,
        entity: CommentEntity,
        user: UserRecord,
      ) = {
      val comment = commentDao.findById(recordId).await.get
      assertCommentResponse(entity, comment, user)
    }

    def assertCommentDeleted(commentId: UUID) = afterAWhile(commentDao.findById(commentId).await must beNone)

    def assertCommentWasntDeleted(commentId: UUID) = commentDao.findById(commentId).await must beSome
  }

}
