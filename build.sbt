name := """play-getting-started"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  ws,
  "com.google.cloud" % "google-cloud-firestore" % "1.7.0",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"
)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _ )
