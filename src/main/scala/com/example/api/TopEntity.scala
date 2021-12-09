package com.example.api

import com.example.db.SubEntity

case class TopEntity(
  id: Long,
  name: String,
  sub: SubEntity
)
