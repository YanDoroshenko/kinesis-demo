package com.github.yandoroshenko.kinesisdemo.model

final case class Event[T](timestamp: Long, eventType: String, value: T)
