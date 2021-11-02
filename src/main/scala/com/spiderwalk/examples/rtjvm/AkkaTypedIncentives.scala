package com.spiderwalk.examples.rtjvm

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

/**
 *
 *
 * @author Jev Prentice
 * @since 29 October 2021
 */
object AkkaTypedIncentives {

  // 1 - typed messages and actors
  sealed trait ShoppingCartMessage

  case class AddItem(item: String) extends ShoppingCartMessage

  case class RemoveItem(item: String) extends ShoppingCartMessage

  case object ValidateCart extends ShoppingCartMessage

  val shoppingRootActor = ActorSystem(
    Behaviors.receiveMessage[ShoppingCartMessage] { message: ShoppingCartMessage =>
      message match {
        case AddItem(item) => println(s"Adding $item to cart")
        case RemoveItem(item) => println(s"Removing $item from cart")
        case ValidateCart => println("The cart is good")
      }
      Behaviors.same
    },
    "simpleShoppingActor"
  )

  shoppingRootActor ! ValidateCart

  // 2 - mutable state

  val shoppingRootActor2 = ActorSystem(

    Behaviors.setup[ShoppingCartMessage] { ctx =>

      // mutable local state
      var items: Set[String] = Set()

      Behaviors.receiveMessage[ShoppingCartMessage] { message: ShoppingCartMessage =>
        message match {
          case AddItem(item) => println(s"Adding $item to cart")
            items += item
          case RemoveItem(item) => println(s"Removing $item from cart")
            items -= item
          case ValidateCart => println("The cart is good")
        }
        Behaviors.same
      }
    }
    ,
    "simpleShoppingActorMutable"
  )

  def shoppingBehavior(items: Set[String]): Behavior[ShoppingCartMessage] =
    Behaviors.receiveMessage[ShoppingCartMessage] {
      case AddItem(item) => println(s"Adding $item to cart")
        shoppingBehavior(items + item)
      case RemoveItem(item) => println(s"Removing $item from cart")
        shoppingBehavior(items - item)
      case ValidateCart => println("The cart is good")
        Behaviors.same
    }


  // 3 - hierarchy

  val rootOnlineStoreActor = ActorSystem(
    Behaviors.setup[ShoppingCartMessage] { ctx =>
      // create children here
      ctx.spawn(shoppingBehavior(Set()), "danielsShoppingCart")
      Behaviors.empty
    }, "onlineStore"
  )
  // You can only create children actors, no more system.actorOf !
}
