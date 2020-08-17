package io.paytouch.core.json.serializers

import java.util.UUID

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.paytouch.core.utils.PaytouchRouteSpec
import org.json4s._
import org.json4s.native.Serialization

class UUIDKeySerializationSpec extends PaytouchRouteSpec {

  "Configured Serializers" should {
    "de/serialize UUID keys properly" in {
      val uuid = UUID.randomUUID
      val data = JObject(JField(uuid.toString, JInt(1)))

      data.extract[Map[UUID, Int]] ==== Map(uuid -> 1)
      Serialization.write(data) ==== s"""{"$uuid":1}"""
    }
  }
  "Configured Marshallers" should {
    "un/marshal UUID keys properly" in {
      val uuid = UUID.randomUUID
      val data = Map(uuid -> 1)

      Unmarshal(Marshal(data).to[RequestEntity].await).to[Map[UUID, Int]].await ==== data
    }
  }
}
