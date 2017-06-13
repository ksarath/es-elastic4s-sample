package com.es.core

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.PutMappingDefinition

object SampleMapping extends ESMapping {
  protected override val indexName: String = "myindex"

  protected override val mappingName: String = "articles"

  protected override val mappingDefinition: PutMappingDefinition = {
    putMapping(indexName / mappingName) fields (
      keywordField("id") index true,
      textField("title") index false fields (keywordField("keyword") normalizer ("lowerCaseNormalizer")),
      keywordField("tags") index true,
      nestedField("author") fields (
        keywordField("id") index true,
        textField("name") index false fields (keywordField("keyword") normalizer ("lowerCaseNormalizer"))
      )
    )
  }
}
