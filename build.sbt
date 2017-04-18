name := "play-websocket-java"

version := "1.0"

scalaVersion := "2.11.11"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies += javaWs
libraryDependencies += "org.webjars" % "bootstrap" % "2.3.2"
libraryDependencies += "org.webjars" % "flot" % "0.8.3"

// Testing libraries for dealing with CompletionStage...
libraryDependencies += "org.assertj" % "assertj-core" % "3.4.1" % Test
libraryDependencies += "com.jayway.awaitility" % "awaitility" % "1.7.0" % Test

val akkaVersion = "2.4.11"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test

LessKeys.compress := true
