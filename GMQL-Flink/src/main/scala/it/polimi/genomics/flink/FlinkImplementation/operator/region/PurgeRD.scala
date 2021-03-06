package it.polimi.genomics.flink.FlinkImplementation.operator.region

import it.polimi.genomics.core.DataStructures.{MetaOperator, RegionOperator}
import it.polimi.genomics.core.DataTypes.FlinkRegionType
import it.polimi.genomics.core.exception.SelectFormatException
import it.polimi.genomics.flink.FlinkImplementation.FlinkImplementation
import org.apache.flink.api.scala._
import org.slf4j.LoggerFactory

/**
 * Created by michelebertoni on 05/05/15.
 */
object PurgeRD {

  final val logger = LoggerFactory.getLogger(this.getClass)
  @throws[SelectFormatException]
  def apply(executor : FlinkImplementation, metaDataset : MetaOperator, inputDataset : RegionOperator, env : ExecutionEnvironment) : DataSet[FlinkRegionType] = {
    val metaIdsList = executor.implement_md(metaDataset, env).distinct(0).map(_._1).collect
    executor.implement_rd(inputDataset, env).filter((a : FlinkRegionType) => metaIdsList.contains(a._1))
  }
}
