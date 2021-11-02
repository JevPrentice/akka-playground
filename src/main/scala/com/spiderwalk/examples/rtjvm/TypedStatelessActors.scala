package com.spiderwalk.examples.rtjvm

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

/**
 *
 *
 * @author Jev Prentice
 * @since 30 October 2021
 */
object TypedStatelessActors {

  trait SimpleThing

  case object EatChocolate extends SimpleThing

  case object WashDishes extends SimpleThing

  case object LearnAkka extends SimpleThing

  val emotionalMutableActor: Behavior[SimpleThing] = Behaviors.setup { context =>
    // spin up the actor state
    var happiness = 0

    // behaviour of the actor
    Behaviors.receiveMessage {
      case EatChocolate =>
        context.log.info(s"($happiness) eating chocolate, yum")
        happiness += 1
        Behaviors.same
      case WashDishes =>
        context.log.info(s"($happiness) doing a chore, womp womp...")
        happiness -= 2
        Behaviors.same
      case LearnAkka =>
        context.log.info(s"($happiness) learning akka this is cool")
        happiness += 100
        Behaviors.same
      case _ =>
        context.log.info(s"($happiness) received something i don't know about")
        Behaviors.same
    }
  }

  def emotionalFunctionalActor(happiness: Int = 0): Behavior[SimpleThing] = Behaviors.receive { (context, message) =>
    message match {
      case EatChocolate =>
        context.log.info(s"($happiness) eating chocolate, yum")
        emotionalFunctionalActor(happiness + 1)
      case WashDishes =>
        context.log.info(s"($happiness) doing a chore, womp womp...")
        emotionalFunctionalActor(happiness - 2)
      case LearnAkka =>
        context.log.info(s"($happiness) learning akka this is cool")
        emotionalFunctionalActor(happiness + 100)
      case _ =>
        context.log.info(s"($happiness) received something i don't know about")
        Behaviors.same
    }
  }
  // this is not actually a true recursive call!

  def main(args: Array[String]): Unit = {

    //    val emotionalActorSystem = ActorSystem(emotionalMutableActor, "EmotionalActor")

    val emotionalActorSystem = ActorSystem(emotionalFunctionalActor(), "EmotionalActor")

    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! EatChocolate
    emotionalActorSystem ! WashDishes
    emotionalActorSystem ! LearnAkka

    Thread.sleep(1000)
    emotionalActorSystem.terminate()
  }
}
