

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

/**
 * COS connection object
 */

object COS {
  def connect(spark: SparkSession) {
    val sc = spark.sparkContext
    
    sc.hadoopConfiguration.set("fs.cos.impl", "com.ibm.stocator.fs.ObjectStoreFileSystem")

    sc.hadoopConfiguration.set("fs.stocator.scheme.list", "cos")
    sc.hadoopConfiguration.set("fs.stocator.cos.impl", "com.ibm.stocator.fs.cos.COSAPIClient")
    sc.hadoopConfiguration.set("fs.stocator.cos.scheme", "cos")
    
    sc.hadoopConfiguration.set("fs.cos.mycos.access.key", "0aba66146f3b450cacebaa908046d17e")
    sc.hadoopConfiguration.set("fs.cos.mycos.endpoint", "https://s3.us.cloud-object-storage.appdomain.cloud")
    sc.hadoopConfiguration.set("fs.cos.mycos.secret.key", "<XXXXXX>")
    sc.hadoopConfiguration.set("fs.cos.service.v2.signer.type", "false")
  }
}

/**
 * Assignment module
 */

object Assignment { 
  def main(args: Array[String]) {
    val spark = SparkSession.builder.master("local[*]").appName("Simple Application").getOrCreate()
    
    // Connect to IBM Cloud Object Storage
    COS.connect(spark)
    
    // Read employee dataset
    val df = spark.read.option("header", "true").csv("cos://candidate-exercise.mycos/emp-data.csv")
    
    // Display first 30 records
    df.show(30)

    // Write data to DB2
    df.write
      .mode("overwrite")
      .format("jdbc")
      .option("driver", "com.ibm.db2.jcc.DB2Driver")
      .option("url", "jdbc:db2://dashdb-txn-sbox-yp-lon02-02.services.eu-gb.bluemix.net:50000/BLUDB")
      .option("dbtable", "Employee2")
      .option("user", "bjz74039")
      .option("password", "XXXXX")
      .save()

   // Read data from DB2
   val db2DF = spark.read
      .format("jdbc")
      .option("driver", "com.ibm.db2.jcc.DB2Driver")
      .option("url", "jdbc:db2://dashdb-txn-sbox-yp-lon02-02.services.eu-gb.bluemix.net:50000/BLUDB")
      .option("user", "bjz74039")
      .option("password", "XXXXX")
      .option("dbtable", "Employee2")
      .load()
      
    // Display first 30 records
    db2DF.show(20)
    
    
    // Drop duplicates, Fill Na values, Convert salary to numeric
    val db2DF_std = db2DF.dropDuplicates()
        .na.fill("NA")
        .withColumn("numSalary", regexp_replace(col("Salary"), "[$,]", "").cast(DoubleType))

    // Gender ratio in each department
    val gender_ratio_df = db2DF_std.groupBy("Department")
        .pivot("Gender").count()
        .withColumn("Total", col("Female") + col("Male") + col("NA"))
        .withColumn("FemaleRatio", round( col("Female") / col("Total"), 2))
        .withColumn("MaleRatio", round( col("Male") / col("Total"), 2))
    
    gender_ratio_df.show(100)
        
    // Average salary in each department
    val avg_salary_df = db2DF_std.groupBy("Department").agg(round(mean("numSalary").alias("avgSalary"), 2))
    
    avg_salary_df.show(100)
    
    // Salary difference
    val sal_diff_df = db2DF_std.groupBy("Department")
        .pivot("Gender").agg(round(mean("numSalary"), 0))
        .withColumn("Diff(Male-Female)", col("Male") - col("Female"))
        
    sal_diff_df.show(100)
    
    // Write gender ratio to COS
    gender_ratio_df.coalesce(1).write.mode("overwrite").option("header", "true").csv("cos://candidate-exercise.mycos/krishna/gender-ratio")
    val grDF = spark.read.option("header", "true").csv("cos://candidate-exercise.mycos/krishna/gender-ratio")
    grDF.show()
    
    spark.stop()
  }
}