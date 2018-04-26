lazy val dinnerBot = (project in file(".")).
  settings(
    inThisBuild(List(
      name         := "dinner-bot",
      scalaVersion := "2.12.5"
    )),
    name := "dinner-bot"
  )

enablePlugins(JavaAppPackaging)