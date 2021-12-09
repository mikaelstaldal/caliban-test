package com.example

import sttp.client3.httpclient.zio.{send, HttpClientZioBackend}
import sttp.client3.{basicRequest, UriContext}
import sttp.model.MediaType
import zio.clock
import zio.test._
import zio.test.environment.Live

import java.time.Duration

object GraphQLSpec extends DefaultRunnableSpec {
  def spec = suite("GraphQL Spec")(
    testM("GraphQL") {
      for {
        _        <- Main.run(List()).fork
        _        <- Live.live(clock.sleep(Duration.ofSeconds(2))) // There is probably a better zio-ish way to do this
        response <-
          send(
            basicRequest
              .post(uri"http://localhost:8090/api/graphql")
              .contentType(MediaType.ApplicationJson)
              .body("""{
                      |    "query": "{topEntities {id name sub {id name}}}",
                      |    "variables": null
                      |}""".stripMargin)
          )
            .map(_.body)
            .provideCustomLayer(HttpClientZioBackend.layer())
      } yield assertTrue(
        response == Right(
          "{\"data\":{\"topEntities\":[{\"id\":1,\"name\":\"First\",\"sub\":{\"id\":1,\"name\":\"One\"}},{\"id\":2,\"name\":\"Second\",\"sub\":{\"id\":1,\"name\":\"One\"}},{\"id\":3,\"name\":\"Third\",\"sub\":{\"id\":2,\"name\":\"Two\"}}]}}"
        )
      )
    }
  )
}
