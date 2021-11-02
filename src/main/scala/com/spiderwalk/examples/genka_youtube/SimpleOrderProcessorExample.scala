package com.spiderwalk.examples.genka_youtube

import akka.actor.typed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.spiderwalk.examples.genka_youtube.Notifier.Notification
import com.spiderwalk.examples.genka_youtube.Shipper.Shipment

object Notifier {
  final case class Notification(orderId: Int, shipmentSuccess: Boolean)

  def apply(): Behavior[Notification] = Behaviors.receive { (context, message) =>
    context.log.info(message.toString)
    Behaviors.same
  }
}

object Shipper {
  final case class Shipment(orderId: Int, product: String, number: Int, replyTo: typed.ActorRef[Notification])

  def apply(): Behavior[Shipment] = Behaviors.receive { (context, message) =>
    context.log.info(message.toString)
    message.replyTo ! Notification(message.orderId, shipmentSuccess = true)
    Behaviors.same
  }
}

object OrderProcessor {

  final case class Order(id: Int, product: String, number: Int)

  //  def apply(): Behavior[Order] = Behaviors.receiveMessage {
  //    case message =>
  //      println(message.toString)
  //      Behaviors.same
  //  }

  //  def apply(): Behavior[Order] = Behaviors.receive {
  //    (context, message) =>
  //      context.log.info(message.toString())
  //      Behaviors.same
  //  }

  def apply(): Behavior[Order] = Behaviors.setup { context =>

    val shipperRef: typed.ActorRef[Shipment] = context.spawn(Shipper(), "shipper")
    val notifierRef: typed.ActorRef[Notification] = context.spawn(Notifier(), "notifier")

    Behaviors.receiveMessage { message =>
      context.log.info(message.toString)
      shipperRef ! Shipment(message.id, message.product, message.number, notifierRef)
      Behaviors.same
    }
  }
}

/**
 * https://www.youtube.com/watch?v=pvDfk2MvO8A&ab_channel=Genka
 *
 * @author Jev Prentice
 * @since 04 October 2021
 */
object SimpleOrderProcessorExample extends App {

  import OrderProcessor._

  val orderProcessor: ActorSystem[OrderProcessor.Order] = ActorSystem(OrderProcessor(), "orders")
  orderProcessor ! Order(1, "Jacket", 2)
  orderProcessor ! Order(2, "Shoes", 1)
  orderProcessor ! Order(3, "Socks", 5)
  orderProcessor ! Order(4, "Umbrella", 3)
}
