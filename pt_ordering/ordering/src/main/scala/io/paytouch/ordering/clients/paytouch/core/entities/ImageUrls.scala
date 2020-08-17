package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.ImageSize

final case class ImageUrls(imageUploadId: UUID, urls: Map[ImageSize, String])
