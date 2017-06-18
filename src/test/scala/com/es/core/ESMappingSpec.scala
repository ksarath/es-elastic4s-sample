package com.es.core

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.sksamuel.elastic4s.testkit.ElasticSugar
import config.TestConfig
import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ESMappingSpec extends TestKit(ActorSystem("testsystem", TestConfig.config))
    with WordSpecLike with Matchers with BeforeAndAfterAll with ElasticSugar {

  object SM extends SampleMapping

  override def beforeAll {
    val status = client.execute {
      clusterHealth()
    }.await.getStatus

    assert(status == ClusterHealthStatus.GREEN || status == ClusterHealthStatus.YELLOW)
  }

  override def afterAll {
    client.close()
    shutdown()
  }

  implicit private val esClient = client

  private def createIndexAndMapping() = {
    val response = SM.applyIndexAndMapping()
    response.onFailure {
      case _ => fail("failed to create index and mapping")
    }

    response map {
      result =>
        result.isAcknowledged shouldBe true

        client execute getMapping(SM.indexName / SM.mappingName) map (
          _.mappings.isEmpty shouldBe false
        )
    }

    Await.result(response, 10 seconds)
  }

  "ESMappingSpec" must {
    "create index and mappings" in {
      within(10 seconds) {
        createIndexAndMapping()
      }
    }
  }
}
