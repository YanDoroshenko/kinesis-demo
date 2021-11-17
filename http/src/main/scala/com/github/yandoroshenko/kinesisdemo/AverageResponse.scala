package com.github.yandoroshenko.kinesisdemo

case class AverageResponse(eventType: String, value: Option[BigDecimal], processedCount: Long)
