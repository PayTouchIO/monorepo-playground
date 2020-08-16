addCommandAlias("root", "project pt_ordering")

lazy val `pt_ordering` =
  project
    .in(file("."))
    .aggregate(ordering)
    .disablePlugins(AssemblyPlugin, DockerPlugin)

lazy val ordering =
  project
    .in(file("ordering"))
    .dependsOn(`authentikat-jwt` % Cctt)
    .dependsOn(domain % Cctt)
    .enablePlugins(SbtLiquibase, DockerPlugin)
    .settings(
      assembly / mainClass := Some("io.paytouch.ordering.Main"),
      assemblySettings,
      dockerSettings("pt_ordering"),
      liquibaseSettings,
      testCleanupSettings,
      libraryDependencies ++=
        Seq(
          `ch.qos.logback`.`logback-classic`,
          // `com.auth0`.`jwks-rsa`,
          `com.beachape`.`enumeratum`,
          `com.github.agilesteel`.`spells`,
          // `com.github.alexarchambault`.`scalacheck-shapeless_1.14`,
          `com.github.seratch`.`awscala`,
          // `com.github.t3hnar`.`scala-bcrypt`,
          `com.github.tminglei`.`slick-pg_json4s`,
          `com.github.tminglei`.`slick-pg`,
          // `com.github.tototoshi`.`scala-csv`,
          // `com.google.api-client`.`google-api-client`,
          // `com.google.zxing`.`core`,
          // `com.google.zxing`.`javase`,
          // `com.malliina`.`scrimage-core`,
          // `com.pauldijou`.`jwt-json4s-native`,
          `com.stripe`.`stripe-java`,
          `com.softwaremill.macwire`.`macros`,
          `com.softwaremill.sttp`.`core`,
          `com.typesafe.akka`.`akka-http`,
          `com.typesafe.akka`.`akka-slf4j`,
          `com.typesafe.akka`.`akka-stream`,
          `com.typesafe.scala-logging`.`scala-logging`,
          `com.typesafe.slick`.`slick-hikaricp`,
          `com.typesafe.slick`.`slick`,
          // `io.bartholomews`.`scala-iso`,
          `io.scalaland`.`chimney`,
          `io.sentry`.`sentry-logback`,
          `io.sentry`.`sentry`,
          `io.underscore`.`slickless`,
          `net.debasishg`.`redisclient`,
          `net.logstash.logback`.`logstash-logback-encoder`,
          `org.sangria-graphql`.`sangria`,
          `org.sangria-graphql`.`sangria-json4s-native`,
          `org.scalacheck`.`scalacheck`,
          `org.typelevel`.`cats-core`,
          `org.yaml`.`snakeyaml`,
        ),
      libraryDependencies ++=
        Seq(
          `com.github.alexarchambault`.`scalacheck-shapeless_1.14`,
          `com.danielasfregola`.`random-data-generator-magnolia`,
          `com.typesafe.akka`.`akka-http-testkit`,
          `com.typesafe.akka`.`akka-testkit`,
          `io.circe`.`circe-json-schema`,
          `io.circe`.`circe-parser`,
          `io.swagger`.`swagger-parser`,
          `org.liquibase`.`liquibase-core`,
          `org.specs2`.`specs2-core`,
          `org.specs2`.`specs2-mock`,
          `org.specs2`.`specs2-scalacheck`,
        ).map(_ % Test),
    )
