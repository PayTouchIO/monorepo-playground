addCommandAlias("root", "project pt_ordering")

lazy val `pt_ordering` =
  project
    .in(file("."))
    .aggregate(ordering)

lazy val ordering =
  project
    .in(file("ordering"))
    .dependsOn(domain % Cctt)
