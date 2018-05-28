name := "scala-stomp-websocket-client"

organization := "org.github.soylent-grin"

version := "0.1.2"

scalaVersion := "2.11.12"

libraryDependencies += "org.java-websocket" % "Java-WebSocket" % "1.3.8"

// POM settings for Sonatype
homepage := Some(url("https://github.com/soylent-grin/scala-stomp-websocket-client"))
scmInfo := Some(ScmInfo(url("https://github.com/soylent-grin/scala-stomp-websocket-client")
                            "git@github.com:soylent-grin/scala-stomp-websocket-client.git"))
developers := List(Developer("soylent-grin",
                             "",
                             "",
                             url("https://github.com/soylent-grin")))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
publishMavenStyle := true

// Add sonatype repository settings
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)