package com.es.core

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.sksamuel.elastic4s.testkit.ElasticSugar
import config.TestConfig
import org.elasticsearch.cluster.health.ClusterHealthStatus
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class NestedAggregationTest extends TestKit(ActorSystem("testsystem", TestConfig.config))
  with WordSpecLike with Matchers with BeforeAndAfterAll with ElasticSugar {

  object SM extends SampleMapping with NestedQuery with NestedAggregation
  import SM.AuthorHitAs

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

  "Nested filter query" must {

    createIndexAndMapping()
    Await.result(SM.insertSampleData(), 20 seconds)
    refreshAll()

    "return aggregated authors" in {
      within(20 seconds) {
        val queryDef = search(SM.indexName / SM.mappingName) aggregations {
          SM.getNestedAggregationQuery(
            NestedAggregationField("authors.name.keyword", "nestedText"),
            Seq(
              SearchFilter("authors.books.name.keyword", "Book 3", "nestedText", "term"),
              SearchFilter("authors.books.publishers.name.keyword", "Publisher 1", "nestedText", "term")
            )
          )
        }

        //println(esClient.show(queryDef))

        val result = Await.result(client execute queryDef, 20 seconds)
        val authors = SM.getAggregationResponse(result) map(_.to[Author])
        authors.size shouldBe 1
        authors(0).id shouldBe "aid 3"
      }
    }

    "return empty aggregated authors with wrong filters" in {
      within(20 seconds) {
        val queryDef = search(SM.indexName / SM.mappingName) aggregations {
          SM.getNestedAggregationQuery(
            NestedAggregationField("authors.name.keyword", "nestedText"),
            Seq(
              SearchFilter("authors.books.name.keyword", "Book 2", "nestedText", "term"),
              SearchFilter("authors.books.publishers.name.keyword", "Publisher 1", "nestedText", "term")
            )
          )
        }

        //println(esClient.show(queryDef))

        val result = Await.result(client execute queryDef, 20 seconds)
        val authors = SM.getAggregationResponse(result) map(_.to[Author])
        authors.size shouldBe 0
      }
    }
  }
}
