package io.paytouch.core.json

import org.json4s.Formats

import io.paytouch.core.json.serializers.CompactPermissionSerializer
import io.paytouch.json.BaseJsonSupport
import io.paytouch.json.json4s.BaseJson4sSupport

object JsonSupport extends JsonSupport

trait JsonSupport extends BaseJsonSupport with BaseJson4sSupport with Json4Formats {
  final lazy val forDb = new JsonSupport {
    override val json4sFormats: Formats = super.json4sFormats + CompactPermissionSerializer
  }
}
