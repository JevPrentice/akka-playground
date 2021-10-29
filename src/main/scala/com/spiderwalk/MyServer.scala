package com.spiderwalk

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

  private val route = get {
    complete(HttpEntity(
      ContentTypes.`text/html(UTF-8)`,
      content
    ))
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("MyServer")

    val host = "0.0.0.0"
    val port = sys.env.getOrElse("port", "80").toInt

    Http().bindAndHandle(route, host, port)
  }
}
