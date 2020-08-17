package io.paytouch.core.filters

import java.util.UUID

import io.paytouch.core.data.model.enums.CommentType

final case class CommentFilters(`type`: Option[CommentType] = None, objectId: Option[UUID] = None) extends BaseFilters
