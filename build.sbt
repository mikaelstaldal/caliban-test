name := "caliban-test"

scalaVersion := "2.13.7"

Compile / mainClass := Some("com.example.Main")

val zioV      = "1.0.12"
val zioQueryV = "0.2.10"
val quillV    = "3.12.0"
val zioHttpV  = "1.0.0.0-RC17"
val calibanV  = "1.3.0"
val sttpV    = "3.3.18"

libraryDependencies ++= Seq(
  "dev.zio"                       %% "zio"                    % zioV,
  "dev.zio"                       %% "zio-query"              % zioQueryV,
  "io.getquill"                   %% "quill-jdbc-zio"         % quillV,
  "io.getquill"                   %% "quill-codegen-jdbc"     % quillV,
  "com.h2database"                 % "h2"                     % "1.4.199",
  "io.d11"                        %% "zhttp"                  % zioHttpV,
  "com.github.ghostdogpr"         %% "caliban"                % calibanV,
  "com.github.ghostdogpr"         %% "caliban-zio-http"       % calibanV,
  "org.slf4j"                      % "slf4j-simple"           % "1.7.32" % "runtime",
  "dev.zio"                       %% "zio-test"               % zioV % "test,it",
  "dev.zio"                       %% "zio-test-sbt"           % zioV % "test,it",
  "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % sttpV % "it"
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings
  )
