package com.spiderwalk

import com.spiderwalk.shop.ShopApi

/**
 *
 *
 * @author Jev Prentice
 * @since 04 October 2021
 */
object Playground extends App {
  println(s"Starting akka-playground with Scala version: ${util.Properties.versionNumberString}")
  ShopApi.start()
}
