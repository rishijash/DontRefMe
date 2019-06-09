package datastore

import java.io.{File, FileInputStream}
import java.util

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.firestore.{FirestoreOptions, QueryDocumentSnapshot}
import javax.inject.Inject
import org.slf4j.LoggerFactory
import models.MetricsSummary
import play.api.Configuration

import scala.collection.JavaConversions._
import java.util.UUID.randomUUID


class UriDataStore @Inject()(config: Configuration) {

  private val log = LoggerFactory.getLogger(this.getClass.getName)

  private val collectionName = "metrics"

  val hostKey = "host"
  val totalParamsCountKey = "totalParamsCount"
  val safeParamsCountKey = "safeParamsCount"


  val f = new File("conf/dontrefmeKey.json")
  private val firebaseClient = FirestoreOptions.newBuilder()
    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream("conf/dontrefmeKey.json")))
    .build().getService

  def createMetrics(host: String, totalParamsCount: Int, safeParamsCount: Int = 0): Boolean = {
    try {
      val docData = new util.HashMap[String, Any](Map(hostKey -> host,
        totalParamsCountKey -> totalParamsCount, safeParamsCountKey -> safeParamsCount))
      val id = randomUUID().toString
      firebaseClient.collection(collectionName).document(id).set(docData)
      true
    } catch {
      case e: Exception =>
        log.error(s"Exception: ${e.getMessage} in creating metrics for host: ${host}, totalParamsCount: ${totalParamsCount}, safeParamsCount: ${safeParamsCount}")
        false
    }
  }

  def getMetricsSummary(): Option[List[QueryDocumentSnapshot]] = {
    try {
      val allData = firebaseClient.collection(collectionName).get().get()
      val allDocuments = allData.getDocuments.toList
      Some(allDocuments)
    } catch {
      case e: Exception =>
        log.error(s"Exception: ${e.getMessage} in getting metrics.")
        None
    }
  }

}
