name := "dinner-bot"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.1"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.11"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0",
  "com.h2database" % "h2" % "1.4.197",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "io.github.nafg" %% "slick-migration-api" % "0.4.0",
  "com.liyaos" %% "scala-forklift-slick" % "0.3.1"
)
