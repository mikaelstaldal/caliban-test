package com.example.db

import caliban.CalibanError
import com.example.Main.DBContext
import com.example.db.DatastoreLive.{FetchAllTopEntities, FetchSubEntityById}
import zio.query._
import zio.{Chunk, Has, ZIO, ZLayer}

import javax.sql.{DataSource => JDBCDataSource}

case class DatastoreLive(jdbcDataSource: JDBCDataSource) extends Datastore {
  override def fetchSubEntity(id: Long): Query[Throwable, SubEntity] =
    ZQuery
      .fromRequest(FetchSubEntityById(id))(SubEntityByIdDataSource)

  override def fetchTopEntities(): Query[Throwable, Seq[TopEntity]] =
    ZQuery
      .fromRequest(FetchAllTopEntities())(AllTopEntitiesDataSource)

  lazy val SubEntityByIdDataSource: DataSource.Batched[Any, FetchSubEntityById] =
    new DataSource.Batched[Any, FetchSubEntityById] {
      val identifier: String                                                               = "SubEntityById"
      def run(requests: Chunk[FetchSubEntityById]): ZIO[Any, Nothing, CompletedRequestMap] = {
        val resultMap = CompletedRequestMap.empty
        requests.toList match {
          case request :: Nil =>
            fetchSubEntityById(request).either.map(result => resultMap.insert(request)(result.map(_._2)))
          case batch          =>
            fetchSubEntitiesByIds(batch)
              .fold(
                (err: Throwable) => requests.foldLeft(resultMap) { case (map, request) => map.insert(request)(Left(err)) },
                _.foldLeft(resultMap) { case (map, entity) => map.insert(FetchSubEntityById(entity.id))(Right(entity)) }
              )
        }
      }
    }

  lazy val AllTopEntitiesDataSource: DataSource.Batched[Any, FetchAllTopEntities] =
    new DataSource.Batched[Any, FetchAllTopEntities] {
      val identifier: String = "AllTopEntities"
      def run(
        requests: Chunk[FetchAllTopEntities]
      ): ZIO[Any, Nothing, CompletedRequestMap] = {
        val resultMap = CompletedRequestMap.empty
        requests.toList match {
          case request :: Nil =>
            fetchAllTopEntities(request).either.map(result => resultMap.insert(request)(result.map(_._2)))
          case batch          =>
            ZIO
              .foreach(batch)(request => fetchAllTopEntities(request))
              .fold(
                (err: Throwable) => requests.foldLeft(resultMap) { case (map, request) => map.insert(request)(Left(err)) },
                _.foldLeft(resultMap) { case (map, (request, baySessions)) => map.insert(request)(Right(baySessions)) }
              )
        }
      }
    }

  import DBContext._

  private def fetchSubEntityById(request: FetchSubEntityById): ZIO[Any, Throwable, (FetchSubEntityById, SubEntity)] = {
    println(s"fetchSubEntityById($request)")
    DBContext
      .run(quote {
        query[SubEntity].filter(c => c.id == lift(request.id))
      })
      .map(_.headOption)
      .someOrFail(CalibanError.ExecutionError(s"SubEntity ${request.id} not found"))
      .map((request, _))
      .provide(Has(jdbcDataSource))
  }

  private def fetchSubEntitiesByIds(requests: List[FetchSubEntityById]): ZIO[Any, Throwable, List[SubEntity]] = {
    println(s"fetchSubEntitiesByIds($requests)")
    DBContext
      .run(quote {
        query[SubEntity].filter(c => liftQuery(requests.map(_.id)).contains(c.id))
      })
      .provide(Has(jdbcDataSource))
  }

  private def fetchAllTopEntities(request: FetchAllTopEntities): ZIO[Any, Throwable, (FetchAllTopEntities, Seq[TopEntity])] = {
    println(s"fetchAllTopEntities($request)")
    DBContext
      .run(quote {
        query[TopEntity]
      })
      .map((request, _))
      .provide(Has(jdbcDataSource))
  }
}

object DatastoreLive {
  def layer: ZLayer[Has[JDBCDataSource], Nothing, Has[Datastore]] =
    ZIO.service[JDBCDataSource].map(DatastoreLive.apply).toLayer

  case class FetchSubEntityById(id: Long) extends Request[Throwable, SubEntity]

  case class FetchAllTopEntities() extends Request[Throwable, Seq[TopEntity]]
}
