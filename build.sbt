lazy val test = Seq(
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
  "org.scalamock" %% "scalamock" % "4.1.0" % "test"
)

lazy val TgAPI = Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.11",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
  "info.mukel" %% "telegrambot4s" % "3.0.14"
)

enablePlugins(FlywayPlugin)
resolvers += Resolver.jcenterRepo

lazy val db = Seq(
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0",
  "com.h2database" % "h2" % "1.4.197"
)

flywayUrl :=  "jdbc:h2:./prod"
flywayUser := "root"
flywayPassword := "secret"
flywayLocations += "classpath:db/migration"

parallelExecution in Test := false

lazy val dinnerBot = (project in file(".")).
  settings(
    inThisBuild(List(
      name         := "dinner-bot",
      scalaVersion := "2.12.5"
    )),
    name := "dinner-bot",

    libraryDependencies += "com.typesafe" % "config" % "1.3.2",

    libraryDependencies ++= test,
    libraryDependencies ++= TgAPI,
    libraryDependencies ++= db

  )
/*libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.1"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.11"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1"*/