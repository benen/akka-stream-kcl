import sbt.Keys.version

val akkaVersion = "2.5.1"

lazy val `akka-streams-kinesis` = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(Seq(
    name := "akka-streams-kinesis",
    version := "1.0",
    scalaVersion := "2.12.2",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.amazonaws" % "amazon-kinesis-client" % "1.7.5",
      "ch.qos.logback" % "logback-classic" % "1.1.7"
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.1" % "it,test",
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "it,test",
      "com.whisk" %% "docker-testkit-scalatest" % "0.9.0" % "it,test",
      "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.0" % "it,test",
      "org.mockito" % "mockito-core" % "2.8.9" % "test"
    )
  ))