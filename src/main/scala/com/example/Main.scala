package com.example

import caliban.ZHttpAdapter
import com.example.api.MyApi
import com.example.db.DatastoreLive
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill._
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.stream.ZStream

import javax.sql.DataSource

object Main extends App {
  object DBContext extends H2ZioJdbcContext(Literal)

  private lazy val config: Config = ConfigFactory.load().getConfig("ctx")

  lazy val dataSourceLayer: ZLayer[Any, Throwable, Has[DataSource]] =
    DataSourceLayer.fromConfig(config).flatMap { ds =>
      // This is ugly, and there is probably a better, more zio-ish and quill-ish way to do it.
      val conn = ds.get.getConnection
      try {
        val statement = conn.createStatement()
        try {
          statement.execute("""
                              |CREATE TABLE SubEntity(
                              |  id BIGINT PRIMARY KEY,
                              |  name VARCHAR(250) NOT NULL
                              |)
                              |""".stripMargin)
          statement.execute("""
                              |CREATE TABLE TopEntity(
                              |  id BIGINT PRIMARY KEY,
                              |  name VARCHAR(250) NOT NULL,
                              |  sub_id BIGINT NOT NULL,
                              |  FOREIGN KEY(sub_id) REFERENCES SubEntity(id)
                              |)
                              |""".stripMargin)
          statement.execute("""
                              |INSERT INTO SubEntity(id, name) VALUES(1, 'One')
                              |""".stripMargin)
          statement.execute("""
                              |INSERT INTO SubEntity(id, name) VALUES(2, 'Two')
                              |""".stripMargin)
          statement.execute("""
                              |INSERT INTO SubEntity(id, name) VALUES(3, 'Three')
                              |""".stripMargin)
          statement.execute("""
                              |INSERT INTO TopEntity(id, name, sub_id) VALUES(1, 'First', 1)
                              |""".stripMargin)
          statement.execute("""
                              |INSERT INTO TopEntity(id, name, sub_id) VALUES(2, 'Second', 1)
                              |""".stripMargin)
          statement.execute("""
                              |INSERT INTO TopEntity(id, name, sub_id) VALUES(3, 'Third', 2)
                              |""".stripMargin)
        } finally statement.close()
      } finally conn.close()
      ZLayer.succeed(ds.get)
    }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    (for {
      myApi       <- ZIO.service[MyApi]
      api          = myApi.api
      interpreter <- api.interpreter
      _           <- Server
                       .start(
                         8090,
                         Http.route {
                           case _ -> Root / "api" / "graphql" => ZHttpAdapter.makeHttpService(interpreter)
                           case _ -> Root / "graphiql"        =>
                             Http.succeed(
                               Response.http(
                                 content = HttpData.fromStream(ZStream.fromResource("graphiql.html")),
                                 headers = List(Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML))
                               )
                             )
                         }
                       )
                       .forever
    } yield ())
      .provideCustomLayer((dataSourceLayer >>> DatastoreLive.layer) >>> MyApi.layer)
      .exitCode
}
