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

class NestedQueryTest extends TestKit(ActorSystem("testsystem", TestConfig.config))
  with WordSpecLike with Matchers with BeforeAndAfterAll with ElasticSugar {

  object SM extends SampleMapping with NestedQuery
  import SM.ArticleHitAs

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

    "return resulting documents" in {
      within(20 seconds) {
        val queryDef = search(SM.indexName / SM.mappingName) query {
          boolQuery() must SM.generateQuery(Seq(
            SearchFilter("title.keyword", "article 1", "normal", "term"),
            SearchFilter("title.keyword", "article 2", "normal", "term"),
            SearchFilter("authors.name.keyword", "author 1", "nestedText", "term"),
            SearchFilter("authors.name.keyword", "author 2", "nestedText", "term"),
            SearchFilter("authors.books.publishers.name.keyword", "Publisher 1", "nestedText", "term"),
            SearchFilter("authors.books.publishers.name.keyword", "Publisher 2", "nestedText", "term")
          ))
        }

        //println(esClient.show(queryDef))

        val result = Await.result(client execute queryDef, 20 seconds)
        val articles = result.hits.toList map(_.to[Article])
        articles.size shouldBe 2

        val articleIds = articles map(_.id)
        articleIds find(_ == "id 1") shouldBe Some("id 1")
        articleIds find(_ == "id 2") shouldBe Some("id 2")
      }
    }

    "return empty with wrong filter combinations" in {
      within(20 seconds) {
        val queryDef = search(SM.indexName / SM.mappingName) query {
          boolQuery() must SM.generateQuery(Seq(
            SearchFilter("title.keyword", "article 1", "normal", "term"),
            SearchFilter("authors.name.keyword", "author 1", "nestedText", "term"),
            SearchFilter("authors.books.publishers.name.keyword", "Publisher 2", "nestedText", "term")
          ))
        }

        //println(esClient.show(queryDef))

        val result = Await.result(client execute queryDef, 20 seconds)
        val articles = result.hits.toList map(_.to[Article])
        articles.size shouldBe 0
      }
    }
  }
}
