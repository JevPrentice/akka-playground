package com.spiderwalk.examples.rtjvm

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import com.spiderwalk.examples.rtjvm.AkkaMessageAdaptation.ShoppingCart.CurrentCart

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 *
 *
 * @author Jev Prentice
 * @since 03 November 2021
 */
object AkkaMessageAdaptation {

  object StoreDomain {
    // never use double for money - for illustration purposes
    case class Product(name: String, price: Double)
  }

  object ShoppingCart {

    import StoreDomain._

    sealed trait Request

    case class GetCurrentCart(cartId: String, replyTo: ActorRef[Response]) extends Request
    // + some others

    sealed trait Response

    case class CurrentCart(cartId: String, items: List[Product]) extends Response
    // + some others

    val db: Map[String, List[Product]] = Map {
      "123-abc-456" -> List(Product("iPhone", 999999), Product("Selfie stick", 35))
    }

    def dummyApply(): Behavior[Request] = Behaviors.receiveMessage {
      case GetCurrentCart(cartId, replyTo) =>
        replyTo ! CurrentCart(cartId, db(cartId))
        Behaviors.same
    }
  }

  object Checkout {

    // this is what we receive from the customer
    sealed trait Request

    final case class InspectSummary(cartId: String, replyTo: ActorRef[Response]) extends Request

    private final case class WrappedShoppingCartResponse(response: ShoppingCart.Response) extends Request

    // + some others

    // this is what we send to the customer
    sealed trait Response

    final case class Summary(cartId: String, amount: Double) extends Response
    // + some others

    def apply(shoppingCart: ActorRef[ShoppingCart.Request]): Behavior[Request] =
      Behaviors.setup { context =>
        val messageAdapter: ActorRef[ShoppingCart.Response]
        = context.messageAdapter(rsp => WrappedShoppingCartResponse(rsp))

        def handlingCheckouts(checkoutsInProgress: Map[String, ActorRef[Response]]): Behavior[Request] =
          Behaviors.receiveMessage {
            case InspectSummary(cartId, customer) =>
              shoppingCart ! ShoppingCart.GetCurrentCart(cartId, messageAdapter)
              handlingCheckouts(checkoutsInProgress + (cartId -> customer))
            case WrappedShoppingCartResponse(response) =>
              response match {
                case CurrentCart(cartId, items) =>
                  val summary = Summary(cartId, items.map(_.price).sum)
                  val customer = checkoutsInProgress(cartId) // perhaps add some checks here
                  customer ! summary
                  handlingCheckouts(checkoutsInProgress - cartId)
                //Behaviors.same
                // perhaps remove the customer from the map now
              }
            // logic for dealing with response from shopping cart
          }

        // return a behavior
        handlingCheckouts(Map())
      }
  }

  /*
  naive:
  Behaviour[Checkout.Request &&&&&&&&&& ShoppingCart.Request] - BAD!
  each actor needs to support its own "request" type and nothing else!
   */

  // Customer -> Checkout -> ShoppingCart
  //            "frontend"     "backend"

  def main(args: Array[String]): Unit = {

    import Checkout._

    val rootBehavior: Behavior[Any] = Behaviors.setup { context =>
      val shoppingCart = context.spawn(ShoppingCart.dummyApply(), "shopping-cart")
      val customer = context.spawn(Behaviors.receiveMessage[Checkout.Response] {
        case Summary(_, amount) =>
          println(s"Total to pay: $amount - pay by card below")
          Behaviors.same
      }, "customer")

      val checkout = context.spawn(Checkout(shoppingCart), "checkout")

      // start the interaction
      checkout ! InspectSummary("123-abc-456", customer)

      // not important
      Behaviors.empty
    }

    val system = ActorSystem(rootBehavior, "main-app")
    implicit val ec: ExecutionContext = system.dispatchers.lookup(DispatcherSelector.default())
    system.scheduler.scheduleOnce(1.second, () => system.terminate())
  }
}
