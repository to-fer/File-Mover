libraryDependencies += "org.specs2" %% "specs2" % "latest.integration" % "test"

lazy val file_mover = project.in(file(".")).aggregate(watcher)

lazy val watcher = RootProject(uri("https://github.com/to-fer/Watcher"))
