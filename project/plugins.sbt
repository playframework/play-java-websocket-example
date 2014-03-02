// Comment to get more information during initialization
// logLevel := Level.Warn

resolvers += Resolver.typesafeRepo("releases")

resolvers += Resolver.typesafeIvyRepo("snapshots")

resolvers += Resolver.typesafeRepo("snapshots")

resolvers += Resolver.sbtPluginRepo("snapshots")

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-2014-03-02-cb4638a-SNAPSHOT")
