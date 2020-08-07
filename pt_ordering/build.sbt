lazy val `pt_ordering` =
  project
    .in(file("."))
    .aggregate(core, delivery, persistence, server, client)

lazy val core =
  project
    .in(file("server/01-core"))
    .dependsOn(domain % Cctt)

lazy val delivery =
  project
    .in(file("server/02-delivery"))
    .dependsOn(core % Cctt)

lazy val persistence =
  project
    .in(file("server/02-persistence"))
    .dependsOn(core % Cctt)

lazy val `pt_core/client` =
  ProjectRef(file("../pt_core"), "client")

lazy val server =
  project
    .in(file("server/03-server"))
    .dependsOn(delivery % Cctt)
    .dependsOn(persistence % Cctt)
    .dependsOn(`pt_core/client` % Cctt)

lazy val client =
  project
    .in(file("client"))
    .dependsOn(delivery % Cctt)
