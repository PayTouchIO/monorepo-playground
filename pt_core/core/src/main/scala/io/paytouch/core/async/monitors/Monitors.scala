package io.paytouch.core.async.monitors

import java.util.UUID

import akka.actor.{ ActorRef, Props }
import com.softwaremill.macwire._
import _root_.io.paytouch.core.async.sqs.SQSMessageSender
import _root_.io.paytouch.core.async.trackers.EventTracker
import _root_.io.paytouch.core.async.uploaders.ImageRemover
import _root_.io.paytouch.core.services.Services
import io.paytouch.utils.Tagging._

trait Monitors { self: Services =>

  lazy val productMonitor: ActorRef withTag ArticleMonitor =
    system.actorOf(Props(wire[ArticleMonitor]), s"product-monitor-${UUID.randomUUID}").taggedWith[ArticleMonitor]

  lazy val adminAuthenticationMonitor: ActorRef withTag AdminAuthenticationMonitor =
    system
      .actorOf(Props(wire[AdminAuthenticationMonitor]), s"admin-authentication-monitor-${UUID.randomUUID}")
      .taggedWith[AdminAuthenticationMonitor]

  lazy val authenticationMonitor: ActorRef withTag AuthenticationMonitor =
    system
      .actorOf(Props(wire[AuthenticationMonitor]), s"authentication-monitor-${UUID.randomUUID}")
      .taggedWith[AuthenticationMonitor]

  lazy val locationSettingsMonitor: ActorRef withTag LocationSettingsMonitor =
    system
      .actorOf(Props(wire[LocationSettingsMonitor]), s"location-settings-monitor-${UUID.randomUUID}")
      .taggedWith[LocationSettingsMonitor]

  lazy val receivingOrderMonitor: ActorRef withTag ReceivingOrderMonitor =
    system
      .actorOf(Props(wire[ReceivingOrderMonitor]), s"receiving-order-monitor-${UUID.randomUUID}")
      .taggedWith[ReceivingOrderMonitor]

  lazy val productQuantityHistoryMonitor: ActorRef withTag ProductQuantityHistoryMonitor =
    system
      .actorOf(Props(wire[ProductQuantityHistoryMonitor]), s"product-quantity-history-monitor-${UUID.randomUUID}")
      .taggedWith[ProductQuantityHistoryMonitor]

  lazy val stockModifierMonitor: ActorRef withTag StockModifierMonitor =
    system
      .actorOf(Props(wire[StockModifierMonitor]), s"stock-modifier-monitor-${UUID.randomUUID}")
      .taggedWith[StockModifierMonitor]

  lazy val ticketMonitor: ActorRef withTag TicketMonitor =
    system.actorOf(Props(wire[TicketMonitor]), s"ticket-monitor-${UUID.randomUUID}").taggedWith[TicketMonitor]

  lazy val userMonitor: ActorRef withTag UserMonitor =
    system.actorOf(Props(wire[UserMonitor]), s"user-monitor-${UUID.randomUUID}").taggedWith[UserMonitor]

  lazy val eventTracker: ActorRef withTag EventTracker =
    system.actorOf(Props(wire[EventTracker]), s"event-tracker-${UUID.randomUUID}").taggedWith[EventTracker]

  lazy val imageRemover: ActorRef withTag ImageRemover =
    system
      .actorOf(Props(new ImageRemover(s3Client, imagesBucket)), s"image-remover-${UUID.randomUUID}")
      .taggedWith[ImageRemover]

  lazy val messageSender: ActorRef withTag SQSMessageSender =
    system.actorOf(Props[SQSMessageSender](), s"sqs-message-sender-${UUID.randomUUID}").taggedWith[SQSMessageSender]
}
