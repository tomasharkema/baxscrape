
name := "pageparser"

version := "1.0"

organization := "io.harkema"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds")

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "1.0.0"

scalaVersion := "2.11.8"