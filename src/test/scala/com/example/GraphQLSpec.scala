package com.example

import caliban.ResponseValue.{ListValue, ObjectValue}
import caliban.Value.IntValue.LongNumber
import caliban.Value.StringValue
import com.example.Main.dataSourceLayer
import com.example.api.MyApi
import com.example.db.DatastoreLive
import zio.ZIO
import zio.test._

object GraphQLSpec extends DefaultRunnableSpec {
  def spec = suite("Testing GraphQL layer directly")(
    testM("GraphQL") {
      (for {
        myApi       <- ZIO.service[MyApi]
        api          = myApi.api
        interpreter <- api.interpreter
        response <- interpreter.execute("{topEntities {id name sub {id name}}}")
      } yield
        assertTrue(
          response.data ==
            ObjectValue(List("topEntities" -> ListValue(List(
              ObjectValue(List("id" -> LongNumber(1), "name" -> StringValue("First"), "sub" -> ObjectValue(List("id" -> LongNumber(1), "name" -> StringValue("One"))))),
              ObjectValue(List("id" -> LongNumber(2), "name" -> StringValue("Second"), "sub" -> ObjectValue(List("id" -> LongNumber(1), "name" -> StringValue("One"))))),
              ObjectValue(List("id" -> LongNumber(3), "name" -> StringValue("Third"), "sub" -> ObjectValue(List("id" -> LongNumber(2), "name" -> StringValue("Two")))))
            ))))
        ))
        .provideCustomLayer((dataSourceLayer >>> DatastoreLive.layer) >>> MyApi.layer)
    }
  )
}
