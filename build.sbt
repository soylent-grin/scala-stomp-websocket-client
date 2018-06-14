name := "scala-stomp-websocket-client"

organization := "com.github.vooolll"

version := "0.1.2"

scalaVersion := "2.11.12"

libraryDependencies += "org.java-websocket" % "Java-WebSocket" % "1.3.8"

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

organizationHomepage := None

description := "Scala STOMP web socket client"

autoAPIMappings := true

pomExtra :=
  <url>https://github.com/soylent-grin/scala-stomp-websocket-client</url>
    <scm>
      <url>https://github.com/soylent-grin/scala-stomp-websocket-client</url>
      <connection>scm:git:git@github.com:soylent-grin/scala-stomp-websocket-client.git</connection>
    </scm>
    <developers>
      <developer>
        <id>soylent-grin</id>
        <name>Nikolay Klimov</name>
        <url>https://github.com/soylent-grin</url>
      </developer>
    </developers>

licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))
