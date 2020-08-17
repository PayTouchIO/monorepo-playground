package io.paytouch.core.entities

final case class PusherAuthentication(channelName: String, socketId: String)

final case class PusherToken(auth: String)
