import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{GBTRegressor}
import org.apache.spark.sql.{DataFrame, SparkSession}

object PricePaidRecords extends Serializable{
  def main(args: Array[String]): Unit = {
    //创建SparkSession(是对SparkContext的包装和增强)

    val time1=System.currentTimeMillis()
    val date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)

    @transient val spark: SparkSession = SparkSession.builder()
      .appName(this.getClass.getSimpleName)
      .master("local[4]")     //spark://master:7077   local[*]
      .getOrCreate()              //设置单线程、多线程、分布式模式

    spark.sparkContext.setLogLevel("WARN")

    val df:DataFrame = spark.read
      .option("header","true")
      .option("inferSchema","true")
      .csv("/Users/dry/scala_new/archive/train_timeseries/train_timeseries.csv")    // end of file

    df.printSchema()       //输出数据结构信息
    df.show(10)

    val df2 = df.drop("fips")
    val df3 = df2.drop("date")

    val df4 = df3.na.fill(value = 0.0,cols = Array("score"))     //删除两个无用列，对score列 null 值填充0
    df4.show(10)

    //将表中的影响因子做为features输入，使用GBDT算法训练模型，进行预测
    val assembler = new VectorAssembler().setInputCols(Array("PRECTOT", "PS", "QV2M", "T2M", "T2MDEW", "T2MWET", "T2M_MAX", "T2M_MIN", "T2M_RANGE", "TS", "WS10M", "WS10M_MAX", "WS10M_MIN", "WS10M_RANGE", "WS50M", "WS50M_MAX", "WS50M_MIN", "WS50M_RANGE")).setOutputCol("features")
    // 模型参数设置，设置最大迭代次数等
    val gbdt = new GBTRegressor().setLabelCol("score").setFeaturesCol("features").setMaxIter(10).setMaxDepth(5).setMaxBins(32).setMinInstancesPerNode(1).setSubsamplingRate(1)

    val pipeline = new Pipeline().setStages(Array(assembler,gbdt))


    val time2=System.currentTimeMillis()
    val date2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)

    val model = pipeline.fit(df4)      // 模型训练

    val time3=System.currentTimeMillis()
    val date3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)

    val df5:DataFrame = spark.read
      .option("header","true")
      .option("inferSchema","true")
      .csv("/Users/dry/scala_new/archive/test_timeseries/test_timeseries.csv")

    val df6 = df5.drop("fips")
    val df7 = df6.drop("date")
    val test = df7.na.fill(value = 0.0,cols = Array("score"))      //删除两个无用列

    val labelsAndPredictions = model.transform(test)
    labelsAndPredictions.select("prediction", "score", "features").show(20)  //将预测结果进行显示

    val time4=System.currentTimeMillis()
    val date4 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)

    println("读取数据: " ,time2-time1)
    println("模型训练: " ,time3-time2)
    println("预测: " ,time4-time3)
    println("总时间: " ,time4-time1)   
    println("date1: " ,date1)
    println("date2: " ,date2)
    println("date3: " ,date3)
    println("date4: " ,date4)

  }
}

