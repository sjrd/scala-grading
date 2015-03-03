val commonSettings = Seq(
  organization := "ch.epfl.lamp",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.5",
  crossScalaVersions := Seq("2.10.4", "2.11.5"),
  scalacOptions ++= Seq("-deprecation", "-feature"),

  homepage := Some(url("https://github.com/sbt-coursera/scala-grading"))
  licenses += ("BSD 3-Clause", url("http://opensource.org/licenses/BSD-3-Clause")),
  organizationHomepage := Some(url("http://lamp.epfl.ch")),

  scmInfo := Some(ScmInfo(
      url("https://github.com/sbt-coursera/scala-grading"),
      "scm:git:git@github.com:sbt-coursera/scala-grading.git",
      Some("scm:git:git@github.com:sbt-coursera/scala-grading.git"))),

  pomExtra := (
    <developers>
      <developer>
        <id>vjovanov</id>
        <name>Vojin Jovanovic</name>
        <url>https://github.com/vjovanov</url>
      </developer>
      <developer>
        <id>sjrd</id>
        <name>Sébastien Doeraene</name>
        <url>https://github.com/sjrd/</url>
      </developer>
    </developers>),

  publishMavenStyle := true,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val `scala-grading-runtime` = project.in(file("runtime")).
  settings(
    description := "Runtime library for scala-grading."
  )

lazy val `scala-grading-agent` = project.in(file("agent")).
  settings(
    description := "Java agent for scala-grading."
  )
