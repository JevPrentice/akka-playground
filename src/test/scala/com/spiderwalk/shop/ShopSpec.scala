package com.spiderwalk.shop

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import com.spiderwalk.shop.Shop.{ItemView, Summary}
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class ShopSpec extends ScalaTestWithActorTestKit(
  s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike {

  private var counter = 0

  def newShopId(): String = {
    counter += 1
    s"shop-$counter"
  }

  "The Shop" should {

    "add item" in {
      val shop = testKit.spawn(Shop(newShopId()))
      val probe = testKit.createTestProbe[StatusReply[Shop.Summary]]
      shop ! Shop.AddItem("foo", 42, List.empty, probe.ref)
      probe.expectMessage(StatusReply.Success(Shop.Summary(Map("foo" -> Shop.ItemView("foo", 42, List.empty)))))
    }

    "reject already added item" in {
      val shop = testKit.spawn(Shop(newShopId()))
      val probe = testKit.createTestProbe[StatusReply[Shop.Summary]]
      shop ! Shop.AddItem("foo", 42, List.empty, probe.ref)
      probe.receiveMessage().isSuccess should ===(true)
      shop ! Shop.AddItem("foo", 13, List.empty, probe.ref)
      probe.receiveMessage().isError should ===(true)
    }

    "remove item" in {
      val shop = testKit.spawn(Shop(newShopId()))
      val probe = testKit.createTestProbe[StatusReply[Shop.Summary]]
      shop ! Shop.AddItem("foo", 42, List.empty, probe.ref)
      probe.receiveMessage().isSuccess should ===(true)
      shop ! Shop.RemoveItem("foo", probe.ref)
      probe.expectMessage(StatusReply.Success(Shop.Summary(Map.empty)))
    }

    "adjust quantity" in {
      val shop = testKit.spawn(Shop(newShopId()))
      val probe = testKit.createTestProbe[StatusReply[Shop.Summary]]
      shop ! Shop.AddItem("foo", 42, List.empty, probe.ref)
      probe.receiveMessage().isSuccess should ===(true)
      shop ! Shop.AdjustItemQuantity("foo", 43, probe.ref)
      probe.expectMessage(StatusReply.Success(Shop.Summary(Map("foo" -> Shop.ItemView("foo", 43, List.empty)))))
    }

    "keep its state" in {
      val shopId = newShopId()
      val shop = testKit.spawn(Shop(shopId))
      val probe = testKit.createTestProbe[StatusReply[Shop.Summary]]
      shop ! Shop.AddItem("foo", 42, List.empty, probe.ref)
      probe.expectMessage(StatusReply.Success(Shop.Summary(Map("foo" -> Shop.ItemView("foo", 42, List.empty)))))

      testKit.stop(shop)

      // start again with same shopId
      val restartedShop = testKit.spawn(Shop(shopId))
      val stateProbe = testKit.createTestProbe[Shop.Summary]
      restartedShop ! Shop.Get(stateProbe.ref)
      stateProbe.expectMessage(Shop.Summary(Map("foo" -> Shop.ItemView("foo", 42, List.empty))))
    }

    "find an item by id" in {
      val shopId = newShopId()
      val shop = testKit.spawn(Shop(shopId))
      val probe = testKit.createTestProbe[StatusReply[Shop.Summary]]
      shop ! Shop.AddItem("foo", 42, List.empty, probe.ref)
      probe.expectMessage(StatusReply.Success(Shop.Summary(Map("foo" -> Shop.ItemView("foo", 42, List.empty)))))

      val getItemProbe = testKit.createTestProbe[Option[Shop.ItemView]]
      shop ! Shop.GetItem("foo", getItemProbe.ref)
      getItemProbe.expectMessage(Some(Shop.ItemView("foo", 42, List.empty)))
    }

    "suggest a product based on a free text string" in {

      val shopId = newShopId()
      val shop = testKit.spawn(Shop(shopId))
      val probe1 = testKit.createTestProbe[StatusReply[Shop.Summary]]

      shop ! Shop.AddItem("Test Product A", 10, List("dry", "normal"), probe1.ref)
      shop ! Shop.AddItem("Test Product B", 10, List("sensitive", "normal"), probe1.ref)
      shop ! Shop.AddItem("Test Product C", 10, List("oily", "normal"), probe1.ref)
      shop ! Shop.AddItem("Test Product D", 10, List("normal"), probe1.ref)
      probe1.receiveMessages(4)

      val probe2 = testKit.createTestProbe[Summary]()

      shop ! Shop.SearchKeyword("i mostly have a dry skin", probe2.ref)
      probe2.expectMessage(Summary(Map("Test Product A" -> Shop.ItemView("Test Product A", 10, List("dry", "normal")))))

      shop ! Shop.SearchKeyword("i think my skin is just normal", probe2.ref)
      probe2.expectMessage(Summary(Map(
        "Test Product A" -> ItemView("Test Product A", 10, List("dry", "normal")),
        "Test Product B" -> ItemView("Test Product B", 10, List("sensitive", "normal")),
        "Test Product C" -> ItemView("Test Product C", 10, List("oily", "normal")),
        "Test Product D" -> ItemView("Test Product D", 10, List("normal")))))
    }

    "have updatable data" in {
      val shopId = newShopId()
      val shop = testKit.spawn(Shop(shopId))
      val probe = testKit.createTestProbe[StatusReply[Shop.Summary]]

      shop ! Shop.AddItem("Test Product A", 10, List("dry", "normal"), probe.ref)
      probe.receiveMessages(1)

      shop ! Shop.UpdateItem("Test Product A", 43, List("dry", "normal"), probe.ref)

      probe.expectMessage(StatusReply.Success(Shop.Summary(Map(
        "Test Product A" -> Shop.ItemView("Test Product A", 43, List("dry", "normal"))
      ))))
    }
  }
}
