name := "akka-playground"

version := "0.1"

scalaVersion := "2.13.6"

Compile / mainClass := Some("com.spiderwalk.Playground")

lazy val akkaVersion = "2.6.17"
lazy val akkaHttpVersion = "10.2.6"
lazy val logBackVersion = "1.2.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % logBackVersion,
  "org.scalatest" %% "scalatest" % "3.2.9" % Test
)