package com.example.api

import com.example.db.SubEntity
import zio.query.Query

case class TopEntity(
  id: Long,
  name: String,
  sub: Query[Throwable, SubEntity]
)
