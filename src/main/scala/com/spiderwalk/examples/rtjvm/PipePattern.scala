package com.spiderwalk.examples.rtjvm

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * https://www.youtube.com/watch?v=Mh-xFgSaQn0&ab_channel=RocktheJVM
 * @author Jev Prentice
 * @since 02 November 2021
 */
object PipePattern {

  // encapsulation

  object Infrastructure {
    private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
    private val db: Map[String, Int] = Map(
      "Daniel" -> 123,
      "Alice" -> 456,
      "Bob" -> 999
    )

    def asyncRetrievePhoneNumber(name: String): Future[Int] =
      Future(db(name))
  }

  trait PhoneCallProtocol

  case class FindAndCallPhoneNumber(name: String) extends PhoneCallProtocol

  case class InitiatePhoneCall(number: Int) extends PhoneCallProtocol

  case class LogPhoneCallFailure(reason: Throwable, name: String) extends PhoneCallProtocol

  val phoneCallInitiatorV1: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
    var nPhoneCalls = 0
    var nFailures = 0
    implicit val ec: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case FindAndCallPhoneNumber(name) =>
        val future: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)
        future.onComplete {
          case Success(number) =>
            // perform phone call
            println(s"Initiating phone call for $number")
            nPhoneCalls += 1
          case Failure(ex) =>
            println(s"Phone call failed for $name $ex")
            nFailures += 1
        }
        Behaviors.same
    }
  }

  // pipe pattern helps to improve the above code!

  // pipe pattern = forward the result of a future back to me as a message
  // + the result of the future will be handled as a message which is ATOMIC so we are no longer breaking encapsulation


  val phoneCallInitiatorV2: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
    var nPhoneCalls = 0
    var nFailures = 0
    implicit val ec: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case FindAndCallPhoneNumber(name) =>

        val future: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)

        context.pipeToSelf(future) {
          case Success(number) =>

            InitiatePhoneCall(number)
          case Failure(ex) =>
            LogPhoneCallFailure(ex, name)
        }
        Behaviors.same
      case InitiatePhoneCall(number) =>
        println(s"Initiating phone call for $number")
        nPhoneCalls += 1 // NOW THIS IS NOT A RACE!!
        Behaviors.same
      case LogPhoneCallFailure(ex, name) =>
        println(s"Phone call failed to $name with $ex")
        nFailures += 1
        Behaviors.same
    }
  }

  // we have now avoided the massive problem of actor encapsulation break!


  // making it stateless


  def phoneCallInitiatorV3(nPhoneCalls: Int = 0, nFailures: Int = 0): Behavior[PhoneCallProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case FindAndCallPhoneNumber(name) =>

          val future: Future[Int] = Infrastructure.asyncRetrievePhoneNumber(name)

          context.pipeToSelf(future) {
            case Success(number) =>

              InitiatePhoneCall(number)
            case Failure(ex) =>
              LogPhoneCallFailure(ex, name)
          }
          Behaviors.same
        case InitiatePhoneCall(number) =>
          println(s"Initiating phone call for $number")
          phoneCallInitiatorV3(nPhoneCalls + 1, nFailures)
        case LogPhoneCallFailure(ex, name) =>
          println(s"Phone call failed to $name with $ex")
          phoneCallInitiatorV3(nPhoneCalls, nFailures + 1)
      }
    }

  def main(args: Array[String]): Unit = {
    val root = ActorSystem(phoneCallInitiatorV3(), "phoneCaller")
    root ! FindAndCallPhoneNumber("Alice")
    root ! FindAndCallPhoneNumber("asd")

    Thread.sleep(1000)

    root.terminate()
  }
  // the pipe pattern can be used to handle async operation (futures) from inside an actors scope
}
