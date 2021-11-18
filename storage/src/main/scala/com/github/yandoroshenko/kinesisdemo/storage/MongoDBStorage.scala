package com.github.yandoroshenko.kinesisdemo.storage

import akka.actor.ActorSystem
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.alpakka.mongodb.scaladsl.MongoSink
import akka.stream.scaladsl.{Flow, Sink}
import com.github.yandoroshenko.kinesisdemo.model.Event
import com.github.yandoroshenko.kinesisdemo.storage.MongoDBStorage.Transformer
import com.mongodb.reactivestreams.client.MongoClients
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

case class StorageEvent(timestamp: Long, eventType: String, value: String)

class MongoDBStorage[T](config: MongoConfig)(implicit actorSystem: ActorSystem, transformer: Transformer[T]) extends Storage[T] {
  private val codecRegistry = fromRegistries(fromProviders(classOf[StorageEvent]), DEFAULT_CODEC_REGISTRY)

  private val client = MongoClients.create(config.url)
  private val db = client.getDatabase(config.database)
  private val collection = db.getCollection(config.collection, classOf[StorageEvent]).withCodecRegistry(codecRegistry)

  override def sink: Sink[Event[T], _] =
    Flow
      .fromFunction { e: Event[T] =>
        StorageEvent(e.timestamp, e.eventType, transformer.transform(e.value))
      }
      .to(MongoSink.insertOne[StorageEvent](collection))
}

object MongoDBStorage {
  trait Transformer[T] {
    def transform(t: T): String
  }

  implicit case object BigDecimalTransformer extends Transformer[BigDecimal] {
    override def transform(t: BigDecimal): String = t.toString
  }
}
