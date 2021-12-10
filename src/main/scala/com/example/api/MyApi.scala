package com.example.api

import caliban.GraphQL.graphQL
import caliban.schema.Annotations.GQLDescription
import caliban.schema.GenericSchema
import caliban.wrappers.Wrappers._
import caliban.{GraphQL, RootResolver}
import com.example.db
import com.example.db.Datastore
import zio.console.Console
import zio.query.Query
import zio.{Has, ZIO, ZLayer}

import scala.language.postfixOps

case class MyApi(datastore: Datastore) extends GenericSchema[Any] {
  case class Queries(
    @GQLDescription("Return all top entities")
    topEntities: Query[Throwable, Seq[TopEntity]]
  )

  val api: GraphQL[Console] =
    graphQL(
      RootResolver(
        Queries(fetchTopEntities)
      )
    ) @@ printErrors

  private def fetchTopEntities =
      datastore.fetchTopEntities()
      .map(entities => entities.map(buildTopEntity))

  private def buildTopEntity(topEntity: db.TopEntity) =
    TopEntity(
      id = topEntity.id,
      name = topEntity.name,
      sub = datastore.fetchSubEntity(topEntity.sub_id)
    )
}

object MyApi {
  def layer: ZLayer[Has[Datastore], Nothing, Has[MyApi]] =
    ZIO.service[Datastore].map(MyApi.apply).toLayer
}
