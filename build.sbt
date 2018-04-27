lazy val test = Seq(
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.scalamock" %% "scalamock" % "4.1.0" % "test"
)

lazy val dinnerBot = (project in file(".")).
  settings(
    inThisBuild(List(
      name         := "dinner-bot",
      scalaVersion := "2.12.5"
    )),
    name := "dinner-bot",

    libraryDependencies += "com.typesafe" % "config" % "1.3.2",

    libraryDependencies ++= test
  )

enablePlugins(JavaAppPackaging)