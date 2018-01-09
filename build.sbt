import Dependencies._

libraryDependencies ~= {
  _.map(_.exclude("org.slf4j", "slf4j-log4j12"))
}

val globalSettings = Seq(
  name := Versions.name,
  organization := Versions.organization,
  homepage := Some(url(Versions.homePage)),
  licenses := Seq((Versions.apacheLicense, url(Versions.apacheLicenseUrl)))
)


lazy val compileSettings = Seq(
  version := Versions.appVersion,
  scalaVersion := Versions.ScalaVersion,
  scalacOptions ++= Seq("-encoding", "UTF-8", s"-target:jvm-${Versions.JDKVersion}", "-feature", "-language:_", "-deprecation", "-unchecked", "-Xlint")
)

fork in run := true

lazy val root = (project in file("."))
  .settings(globalSettings:_*)
  .aggregate(core, app, clients)


lazy val core = (project in file("core"))
  .settings(compileSettings:_*)
  .settings(name := "core")
  .settings(libraryDependencies ++= coreDeps)

lazy val app = (project in file("app"))
  .settings(compileSettings:_*)
  .dependsOn(core)
  .settings(name := "app")
  .settings(libraryDependencies ++= appDeps)

lazy val clients = (project in file("clients"))
  .settings(compileSettings:_*)
  .dependsOn(core)
  .settings(name := "clients")
  .settings(libraryDependencies ++= clientDeps)
