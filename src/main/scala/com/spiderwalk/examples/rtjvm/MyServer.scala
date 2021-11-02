package com.spiderwalk.examples.rtjvm

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

/**
 *
 *
 * @author Jev Prentice
 * @since 29 October 2021
 */
object MyServer {

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

  private val route2 =

    get {
      complete(HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        content
      )) ~
        pathPrefix("api") {
          pathPrefix("product") {
            post {
              complete(HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                content
              ))
            } ~
              get {
                complete(HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  content
                ))
              } ~
              put {
                complete(HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  content
                ))
              } ~
              delete {
                complete(HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  content
                ))
              }
          }
        }
    }

  private val route = get {
    complete(HttpEntity(
      ContentTypes.`text/html(UTF-8)`,
      content
    ))
  }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("MyServer")

    val host = "0.0.0.0"
    val port = sys.env.getOrElse("PORT", "8080").toInt

    Http().bindAndHandle(route, host, port)
  }
}
