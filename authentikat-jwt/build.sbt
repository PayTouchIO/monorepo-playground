libraryDependencies ++= Seq(
  `commons-codec`.`commons-codec`,
  `org.json4s`.`json4s-core`,
  `org.json4s`.`json4s-jackson`,
  `org.json4s`.`json4s-native`,
)

libraryDependencies ++= Seq(
  `org.scalatest`.`scalatest`,
).map(_ % Test)
