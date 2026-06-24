// Scaffold to exercise Zinc's incremental compiler + API extraction over the
// F-bounded / bridge-heavy Java sources in src/main/java.
//
// A small Scala source (src/main/scala) references the Java types so that Zinc
// must extract their API (signatures, bridges, F-bounds) and record name hashes.

ThisBuild / scalaVersion := "3.7.0"

lazy val root = (project in file("."))
  .settings(
    name := "f-bound-stress-test",
    // Keep the analysis chatty so we can see Zinc actually processed the Java API.
    Compile / compileOrder := CompileOrder.JavaThenScala,
    // Both the Java class and the Scala driver define a main; pick one for `run`.
    Compile / mainClass := Some("StressDriver"),
    // Surface incremental-compiler diagnostics.
    Compile / scalacOptions ++= Seq("-explain"),
    // Java sources target a release the installed JDK supports.
    Compile / javacOptions ++= Seq("-encoding", "UTF-8")
  )
