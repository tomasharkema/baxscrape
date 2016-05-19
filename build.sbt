name := "baxscrape"

version := "1.0"

lazy val `baxscrape` = (project in file("."))
  .enablePlugins(PlayScala)
  .aggregate(pageparser)
  .dependsOn(pageparser)

lazy val pageparser = project

scalaVersion := "2.11.8"

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test )

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.2",
  "com.netaporter" %% "scala-uri" % "0.4.14",
  "net.ruippeixotog" %% "scala-scraper" % "1.0.0",
  "com.typesafe.akka" %% "akka-contrib" % "2.4.4"
)

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars" % "jquery" % "2.2.3",
  "org.webjars" % "bootstrap" % "3.3.6"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:postfixOps")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"  