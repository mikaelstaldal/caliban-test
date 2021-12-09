package com.example.db

import com.example.db
import zio.query.Query

trait Datastore {
  def fetchSubEntity(id: Long): Query[Throwable, db.SubEntity]

  def fetchTopEntities(): Query[Throwable, Seq[db.TopEntity]]
}
