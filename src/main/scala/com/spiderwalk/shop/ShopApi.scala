package com.spiderwalk.shop

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import com.spiderwalk.shop.Shop._
import spray.json._

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

sealed trait ProductMessage

trait ShopJsonProtocol extends DefaultJsonProtocol {
  implicit val itemViewFormat: RootJsonFormat[ItemView] = jsonFormat3(ItemView)
  implicit val summaryFormat: RootJsonFormat[Summary] = jsonFormat1(Summary)
  implicit val itemRemovedFormat: RootJsonFormat[ItemRemoved] = jsonFormat2(ItemRemoved)
  implicit val itemUpdatedFormat: RootJsonFormat[ItemUpdated] = jsonFormat4(ItemUpdated)
}

/**
 *
 *
 * @author Jev Prentice
 * @since 01 November 2021
 */
object ShopApi extends ShopJsonProtocol with SprayJsonSupport {

  private val content =
    """
      |<html>
      |<head>
      |</head>
      |<body>
      |This is an html served by Akka HTTP!
      |</body>
      |</html>
      |""".stripMargin

  private val defaultShopId = UUID.fromString("89160cc5-5d17-4143-91ea-328512404197").toString

  private implicit val timeout: FiniteDuration = 3.seconds
  private implicit val system: ActorSystem[Shop.Command] = ActorSystem[Shop.Command](Shop.apply(defaultShopId), "Shop")
  private implicit val scheduler: Scheduler = system.scheduler

  def createRoute: Route = {

    pathPrefix("api" / "product") {
      (get & path("suggestion") & parameter(Symbol("search").as[String]) & extractLog) { (search, log) =>

        val future = system.ask(replyTo => SearchKeyword(search, replyTo))(timeout, scheduler)
        onComplete(future) {
          case Success(value: Summary) =>
            complete(HttpEntity(
              ContentTypes.`application/json`,
              value.toJson.prettyPrint
            ))
          case Failure(ex) =>
            log.error(s"Could not find suggestion product: $ex")
            complete(StatusCodes.InternalServerError)
        }

      } ~
        (get & (path(Segment) | parameter(Symbol("productName").as[String])) & extractLog) { (productName, log) =>

          val future = system.ask(replyTo => Shop.GetItem(productName, replyTo))(timeout, scheduler)
          onComplete(future) {
            case Success(value) =>
              complete(HttpEntity(
                ContentTypes.`application/json`,
                value.toJson.prettyPrint
              ))
            case Failure(ex) =>
              log.error(s"Could not find product: $productName", ex)
              complete(StatusCodes.InternalServerError)
          }

        } ~
        (post & (entity(as[Shop.ItemView]) & extractLog)) { (shopItem, log) =>

          val future: Future[StatusReply[Summary]] = system.ask(
            ref => Shop.AddItem(shopItem.itemId, shopItem.quantity, shopItem.keywordPhrases, ref))(timeout, scheduler)
          onComplete(future) {
            case Success(statusReply) =>
              statusReply match {
                case StatusReply.Success(_) =>
                  complete(StatusCodes.OK)
                case StatusReply.Error(ex: Throwable) =>
                  log.error(s"Error status reply when adding item for: ${shopItem.itemId}", ex)
                  complete(StatusCodes.UnprocessableEntity)
              }
            case Failure(ex) =>
              log.error(s"Failure creating new product: ${shopItem.itemId}", ex)
              complete(StatusCodes.InternalServerError)
          }

        } ~
        (put & entity(as[Shop.ItemView]) & extractLog) { (shopItem, log) =>

          val future = system.ask(
            ref => Shop.UpdateItem(shopItem.itemId, shopItem.quantity, shopItem.keywordPhrases, ref))(timeout, scheduler)
          onComplete(future) {
            case Success(statusReply) =>
              statusReply match {
                case StatusReply.Success(_) =>
                  complete(StatusCodes.OK)
                case StatusReply.Error(ex) =>
                  log.error(s"Error status reply when adding item for: ${shopItem.itemId}", ex)
                  complete(StatusCodes.UnprocessableEntity)
              }
            case Failure(ex) =>
              log.error(s"Could not create update product: ${shopItem.itemId}", ex)
              complete(StatusCodes.InternalServerError)
          }

        } ~
        (get & extractLog) { log =>
          val future = system.ask(Shop.Get)(timeout, scheduler)
          onComplete(future) {
            case Success(value) =>
              complete(HttpEntity(
                ContentTypes.`application/json`,
                value.toJson.prettyPrint
              ))
            case Failure(ex) =>
              log.error(s"Could not create retrieve products", ex)
              complete(StatusCodes.InternalServerError)
          }
        } ~
        (delete & (path(Segment) | parameter(Symbol("productName").as[String])) & extractLog) { (productName, log) =>

          val future = system.ask(replyTo => Shop.RemoveItem(productName, replyTo))(timeout, scheduler)
          onComplete(future) {
            case Success(_) => complete(StatusCodes.OK)
            case Failure(ex) =>
              log.error(s"Could not delete retrieve product $productName", ex)
              complete(StatusCodes.InternalServerError)
          }

        }
    } ~
      get {
        complete(HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          content
        ))
      }
  }

  def start(): Unit = {

    val route: Route = createRoute

    val host = "0.0.0.0"
    val port = sys.env.getOrElse("PORT", "8080").toInt

    Http().newServerAt(host, port).bind(route)
  }

  def main(args: Array[String]): Unit = {
    start()
  }
}
