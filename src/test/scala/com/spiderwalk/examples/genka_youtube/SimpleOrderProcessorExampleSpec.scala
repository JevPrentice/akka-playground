package com.spiderwalk.examples.genka_youtube

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.spiderwalk.examples.genka_youtube.Notifier.Notification
import com.spiderwalk.examples.genka_youtube.Shipper.Shipment
import org.scalatest.wordspec.AnyWordSpecLike

/**
 *
 *
 * @author Jev Prentice
 * @since 04 October 2021
 */
class SimpleOrderProcessorExampleSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A shipper" must {
    "notify to notifier" in {
      val replyProbe = createTestProbe[Notification]()
      val underTest = spawn(Shipper())
      underTest ! Shipment(0, "Jacket", 1, replyProbe.ref)
      replyProbe.expectMessage(Notification(0, shipmentSuccess = true))
    }
  }
}
