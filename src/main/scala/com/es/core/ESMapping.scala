package com.es.core

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.TcpClient
import com.sksamuel.elastic4s.analyzers.{ CustomNormalizerDefinition, LowercaseTokenFilter }
import com.sksamuel.elastic4s.mappings.PutMappingDefinition
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ESMapping {

  protected val indexName: String

  protected val mappingName: String

  protected val mappingDefinition: PutMappingDefinition

  def applyIndexAndMapping()(implicit elasticClient: TcpClient): Future[PutMappingResponse] = {
    elasticClient execute indexExists(indexName) flatMap {
      response =>
        if (!response.isExists) {
          createESIndex flatMap (_ => createESMapping)
        } else {
          createESMapping
        }
    }
  }

  private def createESIndex()(implicit elasticClient: TcpClient): Future[CreateIndexResponse] = {
    elasticClient execute createIndex(indexName).analysis(Nil, Seq(
      CustomNormalizerDefinition(
        "lowerCaseNormalizer",
        LowercaseTokenFilter
      )
    ))
  }

  private def createESMapping()(implicit elasticClient: TcpClient): Future[PutMappingResponse] = {
    elasticClient execute mappingDefinition
  }
}
