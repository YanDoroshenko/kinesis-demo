package com.github.yandoroshenko.kinesisdemo.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import com.github.yandoroshenko.kinesisdemo.model.implicits.bigDecimalParser

import scala.util.Success

class EventParserSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {
  "Event parser" should {
    "parse correct string" in {
      forAll { (timestamp: Long, eventType: String, value: BigDecimal) =>
        val str = s"$timestamp,$eventType,$value"
        EventParser.parse[BigDecimal](str) shouldEqual Success(Event[BigDecimal](timestamp, eventType, value))
      }
    }

    "fail on incorrect length" in {
      forAll { (timestamp: Long, eventType: String, value: BigDecimal, x: Long) =>
        val str = s"$timestamp,$eventType,$value,$x"
        val r = EventParser.parse[BigDecimal](str)
        r.isFailure shouldBe true
      }
    }

    "fail on invalid timestamp" in {
      forAll { (timestamp: String, eventType: String, value: BigDecimal) =>
        val str = s"$timestamp,$eventType,$value"
        EventParser.parse[BigDecimal](str).isFailure shouldBe true
      }
    }

    "fail on invalid value" in {
      forAll { (timestamp: Long, eventType: String, value: String) =>
        val str = s"$timestamp,$eventType,$value"
        EventParser.parse[BigDecimal](str).isFailure shouldBe true
      }
    }
  }
}
