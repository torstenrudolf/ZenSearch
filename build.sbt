name := "ZenDeskSearchCodingChallenge"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= List(
  "io.circe" %% "circe-parser" % "0.13.0",
  "io.circe" %% "circe-optics" % "0.13.0",
  "com.lihaoyi" %% "ammonite-terminal" % "2.0.4",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.typelevel" %% "cats-core" % "2.1.1",
  "com.lihaoyi" %% "utest" % "0.7.2" % "test"
)

testFrameworks += new TestFramework("utest.runner.Framework")


mainClass  := Some("zenSearch.ZenSearchApp")

enablePlugins(DockerPlugin)

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

imageNames in docker := Seq(ImageName(s"trudolf/zensearch:latest"))
