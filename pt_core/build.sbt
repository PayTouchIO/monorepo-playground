lazy val `pt_core` =
  project
    .in(file("."))
    .aggregate(
      core,
      delivery,
      persistence,
      server,
      client
    )

lazy val core =
  project
    .in(file("server/01-core"))
    .dependsOn(domain % Cctt)
    // .dependsOn(misc % Cctt)
    // .dependsOn(protocol % Cctt)
    // .dependsOn(util % Cctt)

lazy val delivery =
  project
    .in(file("server/02-delivery"))
    .dependsOn(core % Cctt)

lazy val persistence =
  project
    .in(file("server/02-persistence"))
    .dependsOn(core % Cctt)

lazy val server =
  project
    .in(file("server/03-server"))
    .dependsOn(delivery % Cctt)
    .dependsOn(persistence % Cctt)

lazy val client =
  project
    .in(file("client"))
    .dependsOn(delivery % Cctt)
