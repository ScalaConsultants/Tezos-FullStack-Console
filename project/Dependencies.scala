import sbt._

object Version {

  val akkaHttp                = "10.1.10"
  val akkaHttpCors            = "0.4.1"
  val akkaStream              = "2.5.26"
  val akkaHttpCirce           = "1.29.1"
  val circe                   = "0.12.3"
  val cats                    = "2.0.0"
  val courier                 = "2.0.0"
  val greenMail               = "1.5.11"
  val jodaTime                = "2.10.4"
  val newtype                 = "0.4.3"
  val logback                 = "1.2.3"
  val postgres                = "42.2.8"
  val pureConfig              = "0.12.1"
  val refined                 = "0.9.10"
  val scalactic               = "3.0.5"
  val scalaTest               = "3.0.5"
  val sl4j                    = "1.7.26"
  val slick                   = "3.3.1"
  val tapir                   = "0.12.12"
  val tapirModel              = "1.0.0-RC1"
  val tesozFCTM               = "0.1"
  val wireMock                = "1.58"
  val testContainersScala     = "0.32.0"
  val testContainersPostgres  = "1.12.1"
  val bCrypt                  = "4.1"
  val flyway                  = "6.1.1"
  val scalaCache              = "0.28.0"

}

object Dependencies {

  val akkaHttp: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http"            % Version.akkaHttp,
    "de.heikoseeberger" %% "akka-http-circe"      % Version.akkaHttpCirce,
    "com.typesafe.akka" %% "akka-http-testkit"    % Version.akkaHttp % "it,test"
  )

  val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core"    % Version.circe,
    "io.circe" %% "circe-generic" % Version.circe,
    "io.circe" %% "circe-parser"  % Version.circe,
    "io.circe" %% "circe-refined" % Version.circe
  )

  val akkaHttpCors: Seq[ModuleID] = Seq(
    "ch.megard" %% "akka-http-cors" % Version.akkaHttpCors
  )

  val akkaStream: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-stream"         % Version.akkaStream,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akkaStream % "it,test"
  )

  val cats: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % Version.cats
  )

  val courier: Seq[ModuleID] = Seq(
    "com.github.daddykotex" %% "courier" % Version.courier
  )

  val greenMail: Seq[ModuleID] = Seq(
    "com.icegreen" % "greenmail" % Version.greenMail % "it,test"
  )

  val jodaTime: Seq[ModuleID] = Seq (
    "joda-time" % "joda-time" % "2.10.4"
  )

  val newType: Seq[ModuleID] = Seq (
    "io.estatico" %% "newtype" % Version.newtype
  )

  val logBack: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % Version.logback
  )

  val postgres: Seq[ModuleID] = Seq(
    "org.postgresql" % "postgresql" % Version.postgres
  )

  val pureConfig: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig" % Version.pureConfig
  )

  val refined: Seq[ModuleID] = Seq(
    "eu.timepit" %% "refined" % Version.refined
  )

  val scalactic: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % Version.scalactic
  )

  val scalaTest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Version.scalaTest % "it,test"
  )

  val sl4j: Seq[ModuleID] = Seq(
    "org.slf4j" % "slf4j-api" % Version.sl4j
  )

  val slick: Seq[ModuleID] = Seq(
    "com.typesafe.slick" %% "slick"          % Version.slick,
    "com.typesafe.slick" %% "slick-hikaricp" % Version.slick
  )

  val tapir: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % Version.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % Version.tapir exclude("com.typesafe.akka", "akka-stream_2.12"),
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Version.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % Version.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % Version.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % Version.tapir exclude("com.typesafe.akka", "akka-stream_2.12"),
    "com.softwaremill.sttp.model" %% "core" % Version.tapirModel,
  )

  val tesozFCTM: Seq[ModuleID] = Seq(
    "io.scalac" %% "tezos-fullstack-console-translation-module" % Version.tesozFCTM
  )

  val wireMock: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" % "wiremock" % Version.wireMock % "it,test"
  )
  
  val testContainers: Seq[ModuleID] = Seq(
    "com.dimafeng"        %%  "testcontainers-scala"  % Version.testContainersScala     % "it,test",
    "org.testcontainers"  %   "postgresql"            % Version.testContainersPostgres  % "it,test"
  )

  val bcrypt: Seq[ModuleID] = Seq(
    "com.github.t3hnar" %% "scala-bcrypt" % Version.bCrypt
  )
  
  val flyway: Seq[ModuleID] = Seq(
    "org.flywaydb" % "flyway-core" % Version.flyway
  )

  val scalaCache: Seq[ModuleID] = Seq(
    "com.github.cb372" %% "scalacache-core" % Version.scalaCache,
    "com.github.cb372" %% "scalacache-caffeine" % Version.scalaCache
  )
}
