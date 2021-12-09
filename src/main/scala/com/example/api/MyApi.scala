package com.example.api

import caliban.GraphQL.graphQL
import caliban.schema.Annotations.GQLDescription
import caliban.schema.GenericSchema
import caliban.wrappers.Wrappers._
import caliban.{GraphQL, RootResolver}
import com.example.db
import com.example.db.Datastore
import zio.Has
import zio.clock.Clock
import zio.console.Console
import zio.query.{Query, ZQuery}

import scala.language.postfixOps

case class MyApi(datastore: Datastore) extends GenericSchema[Any] {

  case class Queries(
    @GQLDescription("Return all top entities")
    topEntities: Query[Throwable, Seq[TopEntity]]
  )

  val api: GraphQL[Console with Clock with Has[Datastore]] =
    graphQL(
      RootResolver(
        Queries(fetchTopEntities)
      )
    ) @@ printErrors

  private def fetchTopEntities =
      datastore.fetchTopEntities()
      .flatMap(entities => ZQuery.collectAll(entities.map(buildTopEntity)))

  private def buildTopEntity(topEntity: db.TopEntity) =
    datastore
      .fetchSubEntity(topEntity.sub_id)
      .map(subEntity =>
        TopEntity(
          id = topEntity.id,
          name = topEntity.name,
          sub = subEntity
        )
      )
}
