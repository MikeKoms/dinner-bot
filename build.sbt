lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.12"

lazy val akkaHttp = Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream"          % akkaVersion
)

lazy val dinnerBot = (project in file(".")).
  settings(
    inThisBuild(List(
      name         := "dinner-bot",
      scalaVersion := "2.12.5"
    )),
    name := "dinner-bot",
    libraryDependencies ++= akkaHttp
  )

enablePlugins(JavaAppPackaging)