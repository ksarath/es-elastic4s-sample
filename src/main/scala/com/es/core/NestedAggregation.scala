package com.es.core

import com.sksamuel.elastic4s.ElasticDsl.{fieldSort, filterAgg, nestedAggregation, termsAggregation, topHitsAggregation}
import com.sksamuel.elastic4s.searches.{RichSearchHit, RichSearchResponse}
import com.sksamuel.elastic4s.searches.aggs.{AggregationDefinition, FilterAggregationDefinition, NestedAggregationDefinition}
import org.elasticsearch.search.aggregations.{Aggregation, Aggregations}
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested
import org.elasticsearch.search.aggregations.bucket.terms.{StringTerms, Terms}
import org.elasticsearch.search.aggregations.metrics.tophits.InternalTopHits
import org.elasticsearch.search.sort.SortOrder

import scala.collection.JavaConversions._

case class NestedAggregationField(field: String, fieldType: String = "normal")

trait NestedAggregation {
  this: NestedQuery =>

  def getNestedAggregationQuery(aggField: NestedAggregationField, filters: Seq[SearchFilter]): NestedAggregationDefinition = {
    val topHitsAgg = topHitsAggregation("topHitsAgg") sortBy (fieldSort(aggField.field) order SortOrder.ASC) size 1

    def getSubAggregation(agg: AggregationDefinition, rest: List[FilterAggregationDefinition]): AggregationDefinition = {
      rest match {
        case Nil => agg.subAggregation(topHitsAgg).asInstanceOf[AggregationDefinition]
        case first :: rest => agg.subAggregation(getSubAggregation(first, rest)).asInstanceOf[AggregationDefinition]
      }
    }

    val path1 = aggField.field.substring(0, aggField.field.lastIndexOf("."))
    val path = aggField.fieldType match {
      case "nestedText" => path1.substring(0, path1.lastIndexOf("."))
      case _ => path1
    }

    nestedAggregation("nestedAgg", path) subAggregation (
      termsAggregation("termsAgg") field aggField.field size 10000 order (Terms.Order.term(true)) subAggregation (
        getNestedAggregationFilters(filters) match {
          case Nil => topHitsAgg
          case first :: rest => getSubAggregation(first, rest)
        }
      )
    )
  }

  def getAggregationResponse(response: RichSearchResponse): List[RichSearchHit] = {
    def getResult(aggs: Aggregations): Aggregation = {
      val agg = aggs.asList()(0)
      if (agg.isInstanceOf[InternalFilter]) {
        getResult(agg.asInstanceOf[InternalFilter].getAggregations)
      } else {
        agg
      }
    }

    val nestedAgg = response.aggregations.map.get("nestedAgg").asInstanceOf[Option[InternalNested]]

    nestedAgg match {
      case Some(response) =>
        val termsResult = Option(response.getAggregations.getAsMap.get("termsAgg").asInstanceOf[StringTerms])
        termsResult match {
          case Some(tresult) => {
            val buckets = tresult.getBuckets().toList
            val results = buckets map {
              bucket =>
                val result = getResult(bucket.getAggregations)
                result.asInstanceOf[InternalTopHits].getHits.toList map (RichSearchHit.apply)
            }
            results.flatten
          }
          case _ => List.empty
        }
      case _ => List.empty
    }

  }

  private def getNestedAggregationFilters(filters: Seq[SearchFilter]): Seq[FilterAggregationDefinition] = {
    val queries = filters groupBy(_.field.toLowerCase) map {
      case (key, filterGroup) => filterAgg(s"filter$key", generateFilterQuery(filterGroup))
    }

    queries toSeq
  }
}
