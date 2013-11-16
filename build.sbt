libraryDependencies += "org.specs2" %% "specs2" % "latest.integration" % "test"

lazy val watcher = project.in(file("Watcher"))

lazy val file_mover = project.in(file(".")).dependsOn(watcher)