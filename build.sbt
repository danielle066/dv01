lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := """dv01-loan-stats""",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.9",
    libraryDependencies ++= Seq(
      guice,
      "com.github.tototoshi" %% "scala-csv" % "1.3.5",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    )
  )
