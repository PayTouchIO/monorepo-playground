package io.paytouch
package sbtplugin

import sbt._

object Dependencies {
  trait Binary {
    case object `ch.qos.logback` {
      val `logback-classic` =
        toString % "logback-classic" % "1.2.3"
    }

    case object `com.auth0` {
      val `jwks-rsa` =
        toString % "jwks-rsa" % "0.12.0"
    }

    case object `com.beachape` {
      val `enumeratum` =
        toString %% "enumeratum" % "1.6.1"
    }

    case object `com.danielasfregola` {
      val `random-data-generator-magnolia` =
        toString %% "random-data-generator-magnolia" % "2.8"
    }

    case object `com.github.agilesteel` {
      val `spells` =
        toString %% "spells" % "2.2.2"
    }

    case object `com.github.alexarchambault` {
      val `scalacheck-shapeless_1.14` =
        toString %% "scalacheck-shapeless_1.14" % "1.2.5"
    }

    case object `com.github.seratch` {
      val `awscala` =
        toString %% "awscala" % "0.8.4"
    }

    case object `com.github.t3hnar` {
      val `scala-bcrypt` =
        toString %% "scala-bcrypt" % "4.1"
    }

    case object `com.github.tminglei` {
      val `slick-pg_json4s` = dependency("slick-pg_json4s")
      val `slick-pg` = dependency("slick-pg")

      private def dependency(artifact: String): ModuleID =
        toString %% artifact % "0.19.0"
    }

    case object `com.github.tototoshi` {
      val `scala-csv` =
        toString %% "scala-csv" % "1.3.6"
    }

    case object `com.google.api-client` {
      val `google-api-client` =
        toString % "google-api-client" % "1.30.9"
    }

    case object `com.google.zxing` {
      val `core` = dependency("core")
      val `javase` = dependency("javase")

      private def dependency(artifact: String): ModuleID =
        toString % artifact % "3.4.0"
    }

    case object `com.malliina` {
      val `scrimage-core` =
        toString %% "scrimage-core" % "2.1.10"
    }

    case object `com.pauldijou` {
      val `jwt-json4s-native` =
        toString %% "jwt-json4s-native" % "4.2.0"
    }

    case object `com.stripe` {
      val `stripe-java` =
        toString % "stripe-java" % "19.4.0" // 19.32.0
    }

    case object `com.softwaremill.macwire` {
      val `macros` =
        toString %% "macros" % "2.3.7"
    }

    case object `com.softwaremill.sttp` {
      val `core` =
        toString %% "core" % "1.7.2"
    }

    case object `com.typesafe.akka` {
      val `akka-slf4j` = dependency("akka-slf4j")
      val `akka-stream` = dependency("akka-stream")
      val `akka-testkit` = dependency("akka-testkit")

      private def dependency(artifact: String): ModuleID =
        toString %% artifact % "2.6.8"

      val `akka-http-testkit` = httpDependency("akka-http-testkit")
      val `akka-http` = httpDependency("akka-http")

      private def httpDependency(artifact: String): ModuleID =
        toString %% artifact % "10.1.12"
    }

    case object `com.typesafe.scala-logging` {
      val `scala-logging` =
        toString %% "scala-logging" % "3.9.2"
    }

    case object `com.typesafe.slick` {
      val `slick-hikaricp` = dependency("slick-hikaricp")
      val `slick` = dependency("slick")

      private def dependency(artifact: String): ModuleID =
        toString %% artifact % "3.3.2"
    }

    case object `commons-codec` {
      val `commons-codec` =
        toString % "commons-codec" % "1.14"
    }

    case object `io.bartholomews` {
      val `scala-iso` =
        toString %% "scala-iso" % "0.1.0"
    }

    case object `io.circe` {
      val `circe-json-schema` =
        toString %% "circe-json-schema" % "0.1.0"

      val `circe-parser` =
        toString %% "circe-parser" % "0.13.0"
    }

    case object `io.scalaland` {
      val `chimney` =
        toString %% "chimney" % "0.5.2"
    }

    case object `io.sentry` {
      val `sentry-logback` = dependency("sentry-logback")
      val `sentry` = dependency("sentry")

      private def dependency(artifact: String): ModuleID =
        toString % artifact % "1.7.30"
    }

    case object `io.swagger` {
      val `swagger-parser` =
        toString % "swagger-parser" % "1.0.51"
    }

    case object `io.underscore` {
      val `slickless` =
        toString %% "slickless" % "0.3.6"
    }

    case object `net.debasishg` {
      val `redisclient` =
        toString %% "redisclient" % "3.30"
    }

    case object `net.logstash.logback` {
      val `logstash-logback-encoder` =
        toString % "logstash-logback-encoder" % "6.4"
    }

    case object `org.augustjune` {
      val `context-applied` =
        toString %% "context-applied" % "0.1.4"
    }

    case object `org.liquibase` {
      val `liquibase-core` =
        toString % "liquibase-core" % "3.10.1" // 4.0.0
    }

    case object `org.json4s` {
      val `json4s-core` = dependency("json4s-core")
      val `json4s-jackson` = dependency("json4s-jackson")
      val `json4s-native` = dependency("json4s-native")

      // We need to stay on 3.5.x until https://github.com/json4s/json4s/issues/507
      // which breaks extraction of case classes with type constructors is solved
      private def dependency(artifact: String): ModuleID =
        toString %% artifact % "3.5.5"
    }

    case object `org.postgresql` {
      val `postgresql` =
        toString % "postgresql" % "42.2.14"
    }

    case object `org.sangria-graphql` {
      val `sangria` =
        toString %% "sangria" % "2.0.0"

      val `sangria-json4s-native` =
        toString %% "sangria-json4s-native" % "1.0.1"
    }

    case object `org.scalacheck` {
      val `scalacheck` =
        toString %% "scalacheck" % "1.14.3"
    }

    case object `org.scalatest` {
      val `scalatest` =
        toString %% "scalatest" % "3.2.0"
    }

    case object `org.scalatestplus` {
      val `scalacheck-1-14` =
        toString %% "scalacheck-1-14" % "3.2.0.0"
    }

    case object `org.specs2` {
      val `specs2-core` = dependency("specs2-core")
      val `specs2-mock` = dependency("specs2-mock")
      val `specs2-scalacheck` = dependency("specs2-scalacheck")

      private def dependency(artifact: String): ModuleID =
        toString %% artifact % "4.10.0"
    }

    case object `org.typelevel` {
      val `cats-core` =
        toString %% "cats-core" % "2.1.1"

      val `discipline-scalatest` =
        toString %% "discipline-scalatest" % "1.0.1"

      val `kind-projector` =
        toString %% "kind-projector" % "0.11.0" cross CrossVersion.full
    }

    case object `org.yaml` {
      val `snakeyaml` =
        toString % "snakeyaml" % "1.26"
    }
  }

  trait Source {
    lazy val `authentikat-jwt` =
      ProjectRef(file("../authentikat-jwt"), "authentikat-jwt")

    lazy val domain =
      ProjectRef(file("../domain"), "domain")

    lazy val misc =
      ProjectRef(file("../misc"), "misc")

    lazy val protocol =
      ProjectRef(file("../protocol"), "protocol")

    lazy val util =
      ProjectRef(file("../util"), "util")
  }
}
