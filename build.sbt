name := "reactive-stocks-java8"

version := "1.0-SNAPSHOT"

resolvers += Resolver.typesafeRepo("snapshots")

libraryDependencies ++= Seq(
  javaWs,
  "com.typesafe.akka" %% "akka-actor" % "2.3.0",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.0",
  "org.webjars" %% "webjars-play" % "2.3-2014-03-02-cb4638a-SNAPSHOT",
  "org.webjars" % "bootstrap" % "2.3.1",
  "org.webjars" % "flot" % "0.8.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.0" % "test"
)

play.Project.playJavaSettings

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}
