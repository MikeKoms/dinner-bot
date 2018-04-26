name := "dinner-bot"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.1"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.11"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1"

enablePlugins(JavaAppPackaging)