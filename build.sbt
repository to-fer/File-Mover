libraryDependencies ++= List (
  "org.specs2" %% "specs2" % "latest.integration" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0" % "test",
  "com.twitter" %% "util-logging" % "latest.integration"
)

lazy val file_mover = project.in(file(".")).dependsOn(watcher)

lazy val watcher = RootProject(uri("git://github.com/to-fer/Watcher.git"))
