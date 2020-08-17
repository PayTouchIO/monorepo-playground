package io.paytouch.ordering.data.redis

import io.paytouch.utils.Tagging._
import com.redis._

sealed trait RedisHost
sealed trait RedisPort

class ConfiguredRedis(val host: String withTag RedisHost, port: Int withTag RedisPort) {
  def connect() = new RedisClient(host, port)
}
