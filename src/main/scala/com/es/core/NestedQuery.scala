package com.es.core

import com.sksamuel.elastic4s.ElasticDsl.{boolQuery, matchQuery, nestedQuery, termQuery, wildcardQuery}
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

case class SearchFilter(field: String, value: String, fieldType: String = "normal", queryType: String = "term")

trait NestedQuery {
  def generateQuery(filters: Seq[SearchFilter]): Seq[QueryDefinition] = {
    val queries = filters groupBy(_.field) map {
      case (_, filterGroup) =>
        boolQuery() should
          filterGroup.foldLeft(Seq[QueryDefinition]()) {
            case (queyList, filter) =>
              val query = filter.fieldType match {
                case "normal" => getQuery(filter)
                case "nested" => getNestedQuery(filter, filter.field)
                case "nestedText" if filter.queryType == "term" => getNestedMultiFieldQuery(filter)
                case "nestedText" => getNestedQuery(filter, filter.field)
                case x => throw new RuntimeException(s"Unknown field type: $x for field: $filter")
              }

              queyList :+ query
          }
    }

    queries toSeq
  }

  private def getNestedQuery(filter: SearchFilter, fieldPath: String): QueryDefinition = {
    val path = fieldPath.substring(0, fieldPath.lastIndexOf("."))
    nestedQuery(path) query getQuery(filter)
  }

  private def getNestedMultiFieldQuery(filter: SearchFilter): QueryDefinition = {
    val fieldPath = filter.field.substring(0, filter.field.lastIndexOf("."))
    getNestedQuery(filter, fieldPath)
  }

  private def getQuery(filter: SearchFilter): QueryDefinition = {
    filter.queryType match {
      case "term" => termQuery(filter.field, filter.value)
      case "wildcard" => wildcardQuery(filter.field, filter.value)
      case "match" => matchQuery(filter.field, filter.value)
      case x => throw new RuntimeException(s"Unknown query type: $x for field: $filter")
    }
  }
}
