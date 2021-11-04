package com.spiderwalk.shop

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}

import scala.concurrent.duration._

object Shop {

  final case class ShopData(itemId: String, quantity: Int, keywordPhrases: List[String]) extends CborSerializable

  final case class State(items: Map[String, ShopData]) extends CborSerializable {

    def hasItem(itemId: String): Boolean =
      items.contains(itemId)

    def isEmpty: Boolean =
      items.isEmpty

    def updateItem(itemId: String, quantity: Int, keywordPhrases: List[String]): State = {
      quantity match {
        case 0 => copy(items = items - itemId)
        case _ => copy(items = items + (itemId -> ShopData(itemId, quantity, keywordPhrases)))
      }
    }

    def removeItem(itemId: String): State = copy(items = items - itemId)

    def toSummary: Summary = Summary(items.map(tup => tup._1 -> ItemView(tup._2.itemId, tup._2.quantity, tup._2.keywordPhrases)))
  }

  object State {
    val empty: State = State(items = Map.empty)
  }

  sealed trait Command extends CborSerializable

  final case class AddItem(itemId: String, quantity: Int, keywordPhrases: List[String], replyTo: ActorRef[StatusReply[Summary]]) extends Command

  final case class RemoveItem(itemId: String, replyTo: ActorRef[StatusReply[Summary]]) extends Command

  final case class AdjustItemQuantity(itemId: String, quantity: Int, replyTo: ActorRef[StatusReply[Summary]]) extends Command

  final case class Get(replyTo: ActorRef[Summary]) extends Command

  final case class GetItem(itemId: String, replyTo: ActorRef[Option[ItemView]]) extends Command

  final case class ItemView(itemId: String, quantity: Int, keywordPhrases: List[String]) extends CborSerializable

  final case class SearchKeyword(search: String, replyTo: ActorRef[Summary]) extends Command

  final case class Summary(items: Map[String, ItemView]) extends CborSerializable

  final case class UpdateItem(itemId: String, quantity: Int, keywordPhrases: List[String], replyTo: ActorRef[StatusReply[Summary]]) extends Command

  sealed trait Event extends CborSerializable {
    def shopId: String
  }

  final case class ItemAdded(shopId: String, itemId: String, quantity: Int, keywordPhrases: List[String]) extends Event

  final case class ItemRemoved(shopId: String, itemId: String) extends Event

  final case class ItemQuantityAdjusted(shopId: String, itemId: String, newQuantity: Int) extends Event

  final case class ItemUpdated(shopId: String, itemId: String, quantity: Int, keywordPhrases: List[String]) extends Event

  def apply(shopId: String): Behavior[Command] = {
    EventSourcedBehavior[Command, Event, State](
      PersistenceId("Shop", shopId),
      State.empty,
      (state, command) =>
        handleCommand(shopId, state, command),
      (state, event) => handleEvent(state, event))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
  }

  private def handleCommand(shopId: String, state: State, command: Command): Effect[Event, State] =
    command match {
      case AddItem(itemId, quantity, keywordPhrases, replyTo) =>
        if (state.hasItem(itemId)) {
          replyTo ! StatusReply.Error(s"Item '$itemId' was already added to this shop")
          Effect.none
        } else if (quantity <= 0) {
          replyTo ! StatusReply.Error("Quantity must be greater than zero")
          Effect.none
        } else {
          Effect
            .persist(ItemAdded(shopId, itemId, quantity, keywordPhrases))
            .thenRun(updatedShop => replyTo ! StatusReply.Success(updatedShop.toSummary))
        }

      case RemoveItem(itemId, replyTo) =>
        if (state.hasItem(itemId)) {
          Effect.persist(ItemRemoved(shopId, itemId))
            .thenRun((updatedShop: State) =>
              replyTo ! StatusReply.Success(updatedShop.toSummary))
        } else {
          replyTo ! StatusReply.Success(state.toSummary) // removing an item is idempotent
          Effect.none
        }

      case AdjustItemQuantity(itemId, quantity, replyTo) =>
        if (quantity <= 0) {
          replyTo ! StatusReply.Error("Quantity must be greater than zero")
          Effect.none
        } else if (state.hasItem(itemId)) {
          Effect
            .persist(ItemQuantityAdjusted(shopId, itemId, quantity))
            .thenRun(updatedShop => replyTo ! StatusReply.Success(updatedShop.toSummary))
        } else {
          replyTo ! StatusReply.Error(s"Cannot adjust quantity for item '$itemId'. Item not present in shop")
          Effect.none
        }

      case Get(replyTo) =>
        replyTo ! state.toSummary
        Effect.none

      case GetItem(itemId, replyTo) =>
        replyTo ! state.items.get(itemId).map(item => ItemView(item.itemId, item.quantity, item.keywordPhrases))
        Effect.none

      case SearchKeyword(search, replyTo: ActorRef[Summary]) =>
        val searchKeywords = search.toLowerCase.split(" ")
        replyTo ! Summary(state.items
          .filter(tup => tup._2.keywordPhrases.exists(s => searchKeywords.contains(s)))
          .map(tup => tup._1 -> ItemView(tup._2.itemId, tup._2.quantity, tup._2.keywordPhrases)))
        Effect.none

      case UpdateItem(itemId, quantity, keywordPhrases, replyTo: ActorRef[StatusReply[Summary]]) =>
        if (quantity <= 0) {
          replyTo ! StatusReply.Error("Quantity must be greater than zero")
          Effect.none
        } else if (state.hasItem(itemId)) {
          Effect.persist(ItemUpdated(shopId, itemId, quantity, keywordPhrases))
            .thenRun((updatedShop: State) => replyTo ! StatusReply.Success(updatedShop.toSummary))
        } else {
          replyTo ! StatusReply.Error(s"Cannot adjust quantity for item '$itemId'. Item not present in shop")
          Effect.none
        }
    }

  private def handleEvent(state: State, event: Event) = {
    event match {
      case ItemAdded(_, itemId, quantity, keywordPhrases) => state.updateItem(itemId, quantity, keywordPhrases)
      case ItemRemoved(_, itemId) => state.removeItem(itemId)
      case ItemQuantityAdjusted(_, itemId, quantity) =>
        val keywordPhrases = state.items.get(itemId).map(_.keywordPhrases).getOrElse(List.empty)
        state.updateItem(itemId, quantity, keywordPhrases)
      case ItemUpdated(_, itemId, quantity, keywordPhrases) =>
        state.updateItem(itemId, quantity, keywordPhrases)
    }
  }
}
