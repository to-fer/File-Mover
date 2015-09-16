libraryDependencies ++= List (
  "org.specs2" %% "specs2" % "latest.integration" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "latest.integration" % "test",
  "com.twitter" %% "util-logging" % "latest.integration"
)

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

scalaVersion := "2.11.7"