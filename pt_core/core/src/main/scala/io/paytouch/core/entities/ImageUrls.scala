package io.paytouch.core.entities

import java.util.UUID

final case class ImageUrls(imageUploadId: UUID, urls: Map[String, String])
