
name := "es-elastic4s-sample"

version := "0.1"

scalaVersion := "2.11.11"

val elastic4sV = "5.4.0"

val akkaV      = "2.5.2"

libraryDependencies ++= Seq(
    "com.sksamuel.elastic4s"            %% "elastic4s-core"         % elastic4sV,
    "com.sksamuel.elastic4s"            %% "elastic4s-tcp"          % elastic4sV,
    "com.sksamuel.elastic4s"            %% "elastic4s-testkit"      % elastic4sV      % "test",
    "io.spray"                          %% "spray-json"             % "1.3.3",
    "joda-time"                         %  "joda-time"              % "2.9.9",
    "io.netty"                          %  "netty"                  % "3.10.6.Final",
    "org.apache.httpcomponents"         %  "httpclient"             % "4.5.3",
    "com.carrotsearch"                  %  "hppc"                   % "0.7.2",
    "com.github.spullara.mustache.java" % "compiler"                % "0.9.2",
    "com.tdunning"                      %  "t-digest"               % "3.1",
    "org.slf4j"                         %  "slf4j-simple"           % "1.7.25",
    "com.typesafe.akka"                 %% "akka-testkit"           % akkaV           % "test",
    "org.scalatest"                     %% "scalatest"              % "3.0.1"         % "test"
)

