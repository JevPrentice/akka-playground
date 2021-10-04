name := "akka-playground"

version := "0.1"

scalaVersion := "2.13.6"

lazy val akkaVersion = "2.6.16"
lazy val logBackVersion = "1.2.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % logBackVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.9" % Test
)