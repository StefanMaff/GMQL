package it.polimi.genomics.manager

import java.io.File
import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.Date

import it.polimi.genomics.core.{BinSize, GMQLOutputFormat, GMQLScript, ImplementationPlatform}
import it.polimi.genomics.manager.Launchers.{GMQLLocalLauncher, GMQLSparkLauncher}
import it.polimi.genomics.repository.FSRepository.{DFSRepository, LFSRepository}
import it.polimi.genomics.repository.Utilities
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by abdulrahman on 31/01/2017.
  */
object CLI {
  private final val logger = LoggerFactory.getLogger(this.getClass) //GMQLExecuteCommand.getClass);

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  System.setProperty("current.date", dateFormat.format(new Date()));

  private final val SYSTEM_TMPE_DIR = System.getProperty("java.io.tmpdir")
  private final val DEFAULT_SCHEMA_FILE:String = "/test.schema";
  private final val date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

  private final val usage = "GMQL-Submit " +
    " [-username USER] " +
    "[-exec FLINK|SPARK] [-binsize BIN_SIZE] [-jobid JOB_ID] " +
    "[-verbuse true|false] " +
    "[-outputFormat GTF|TAB]" +
    "-scriptpath /where/gmql/script/is \n" +
    "\n" +
    "\n" +
    "Description:\n" +
    "\t[-username USER]\n" +
    "\t\tThe default user is the user the application is running on $USER.\n" +
    "\n" +
    "\t[-exec FLINK|SPARK] \n" +
    "\t\tThe execution type, Currently Spark and Flink engines are supported as platforms for executing GMQL Script.\n" +
    "\n" +
    "\t[-verbuse true|false]\n" +
    "\t\tThe default will print only the INFO tags. -verbuse is used to show Debug mode.\n" +
    "\n" +
    "\t[-outputFormat GTF|TAB]\n" +
    "\t\tThe default output format is TAB: tab delimited files in the format of BED files." +
    "\n" +
    "\t-scriptpath /where/gmql/script/is/located\n" +
    "\t\tManditory parameter, select the GMQL script to execute"

  def main(args: Array[String]): Unit = {

    //Setting the default options
    var executionType = ImplementationPlatform.SPARK.toString.toLowerCase();
    var scriptPath: String = null;
    var username: String = System.getProperty("user.name")
    var outputPath = ""
    var outputFormat = GMQLOutputFormat.TAB
    var verbuse = false
    var i = 0;

    for (i <- 0 until args.length if (i % 2 == 0)) {
      if ("-h".equals(args(i)) || "-help".equals(args(i))) {
        println(usage)

      } else if ("-exec".equals(args(i))) {
        executionType = args(i + 1).toLowerCase()
        logger.info("Execution Type is set to: " + executionType)

      }  else if ("-username".equals(args(i))) {
        username = args(i + 1).toLowerCase()
        logger.info("Username set to: " + username)

      }  else if ("-verbuse".equals(args(i).toLowerCase())) {
        if(args(i+1).equals("true"))verbuse = true else verbuse = false
        logger.info("Output is set to Verbuse: " + verbuse)

      } else if ("-scriptpath".equals(args(i))) {
        logger.info("scriptpath set to: " + scriptPath)
        val sFile = new File (args(i + 1))
        if(!sFile.exists()) {
          logger.error(s"Script file not found $scriptPath")
          return 0
        };
        scriptPath = sFile.getPath

      } else if ("-outputformat".equals(args(i).toLowerCase())) {
        val out = args(i + 1).toUpperCase().trim
        outputFormat =
          if(out == GMQLOutputFormat.TAB.toString)
            GMQLOutputFormat.TAB
          else if(out == GMQLOutputFormat.GTF.toString)
            GMQLOutputFormat.GTF
          else {
            logger.warn(s"Not knwon format $out, Setting the output format for ${GMQLOutputFormat.TAB}")
            GMQLOutputFormat.TAB
          }
        logger.info(s"Output Format set to: $out" + outputFormat)

      } else
        {
          logger.warn(s"Command option is not found ${args(i)}")
        }
    }

    val gmqlScript = new GMQLScript( new String(Files.readAllBytes(Paths.get(scriptPath))),scriptPath)
    val binSize = new BinSize(5000, 5000, 1000)
    val repository = if(Utilities().MODE == Utilities().HDFS) new DFSRepository() else new LFSRepository()

    val conf = new SparkConf()
      .setAppName("GMQL V2 Spark")
      .setMaster("local[1]")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer").set("spark.kryoserializer.buffer", "64")
      .set("spark.driver.allowMultipleContexts","true")
      .set("spark.sql.tungsten.enabled", "true")
    val sc:SparkContext =new SparkContext(conf)

    val gmqlContext = new GMQLContext(ImplementationPlatform.SPARK, repository, outputFormat, binSize, username,sc)
    val server = GMQLExecute()

    val job = server.registerJob(gmqlScript, gmqlContext, "")
    server.scheduleGQLJobForYarn(job.jobId, new GMQLSparkLauncher(job))
  }
}