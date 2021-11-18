ThisBuild / organization := "com.github.yandoroshenko.kinesis-demo"

ThisBuild / scalaVersion := "2.13.7"

val akkaVersion = "2.6.17"
val alpakkaVersion = "3.0.3"
val circeVersion = "0.14.1"

val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.2.7"
val alpakkaKinesis = "com.lightbend.akka" %% "akka-stream-alpakka-kinesis" % alpakkaVersion
val alpakkaMongo = "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % alpakkaVersion

val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % "1.38.2"

val circeCore = "io.circe" %% "circe-core" % circeVersion
val circeGeneric = "io.circe" %% "circe-generic" % circeVersion

val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
val logback =  "ch.qos.logback" % "logback-classic" % "1.2.3"
val slf4j = "org.slf4j" % "slf4j-simple" % "1.7.32"

val scalatest = "org.scalatest" %% "scalatest" % "3.2.10" % Test
val scalacheck = "org.scalatestplus" %% "scalacheck-1-15" % "3.2.9.0" % Test

lazy val core = (project in file("core")).settings(libraryDependencies ++= Seq(scalatest, scalacheck))

lazy val event =
  (project in file("event")).settings(libraryDependencies ++= Seq(akkaActor, akkaStream, alpakkaKinesis, scalaLogging, scalatest)).dependsOn(core)

lazy val storage = (project in file("storage")).settings(libraryDependencies ++= Seq(akkaStream, alpakkaMongo)).dependsOn(core)

lazy val service = (project in file("service")).settings(libraryDependencies ++= Seq(scalatest)).dependsOn(event, storage)

lazy val http = (project in file("http"))
  .settings(libraryDependencies ++= Seq(akkaActor, akkaStream, akkaHttp, circeCore, circeGeneric, akkaHttpCirce, scalaLogging))

lazy val main = (project in file("main")).settings(libraryDependencies ++= Seq(akkaActor, scalaLogging)).dependsOn(service, http)

lazy val `kinesis-demo` =
  (project in file(".")).aggregate(core, event, storage, service, http, main)
