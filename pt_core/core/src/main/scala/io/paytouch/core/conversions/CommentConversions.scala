package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ CommentUpdate => CommentUpdateModel, _ }
import io.paytouch.core.entities.{ Comment => CommentEntity, CommentUpdate => CommentUpdateEntity, _ }

trait CommentConversions extends ModelConversion[CommentUpdateEntity, CommentUpdateModel] {

  def fromRecordsAndOptionsToEntities(comments: Seq[CommentRecord], userInfos: Map[CommentRecord, UserInfo]) =
    comments.flatMap { comment =>
      userInfos.get(comment).map(userInfo => fromRecordAndOptionsToEntity(comment, userInfo))
    }

  def fromRecordAndOptionsToEntity(comment: CommentRecord, userInfo: UserInfo): CommentEntity =
    CommentEntity(
      id = comment.id,
      user = userInfo,
      body = comment.body,
      createdAt = comment.createdAt,
    )

  def fromUpsertionToUpdate(id: UUID, upsertion: CommentUpdateEntity)(implicit user: UserContext): CommentUpdateModel =
    CommentUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      userId = Some(user.id),
      objectId = upsertion.objectId,
      objectType = upsertion.objectType,
      body = upsertion.body,
    )

}
