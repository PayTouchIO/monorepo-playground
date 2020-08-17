package io.paytouch.core.data.redis

import io.paytouch.core.{ RedisHost, RedisPort }
import io.paytouch.utils.Tagging._
import com.redis._

class ConfiguredRedis(val host: String withTag RedisHost, port: Int withTag RedisPort) {
  def connect() = new RedisClient(host, port)
}
