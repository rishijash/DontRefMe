package engines

import java.net.{URI, URLDecoder}

import datastore.UriDataStore
import javax.inject.Inject
import models.{HostDetails, HostType, MetricsSummary, RemoveParamRes}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.mvc.Request

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UriEngine @Inject()(config: Configuration) {

  val baseUrl = config.getString("url.base").get
  val herokuBaseUrl = config.getString("url.herokuBase").get
  val store = new UriDataStore(config)

  val log = LoggerFactory.getLogger(this.getClass.getName)

  def getRequestUri[T](request: Request[T]): Option[URI] = {
    val uriObj = getUriObj(request.rawQueryString)
    val isSelfUriRequest = (uriObj.toString.contains(baseUrl) || uriObj.toString.contains(herokuBaseUrl))
    if (isSelfUriRequest) None else uriObj
  }

  def removeRefFromUri(uri: String): String = {
    removeRefFromUri(new URI(uri))
  }

  def removeRefFromUri(uriObj: URI): String = {
    try {
      val maybeHostTypeDetails = HostType.getHostTypeDetailsFromHostUriOpt(uriObj.getHost)
      val refRemoverResponse = maybeHostTypeDetails match {
        case Some(hostTypeDetails) => {
          val hostDetails = hostTypeDetails._2
          refRemoverWithRuleEngine(uriObj, hostDetails.safeParams, hostDetails.removeRefFromStringEnd, hostDetails.redirectParams)
        }
        case None => refRemoverWithRuleEngine(uriObj, HostType.commonSafeParams)
      }
      addMetrics(refRemoverResponse.host, refRemoverResponse.totalParams, refRemoverResponse.filterdParams)
      refRemoverResponse.newUri
    } catch {
      case e: Exception => {
        log.error(s"Exception in removing ref: ${e}")
        uriObj.toString
      }
    }
  }

  def getSummary(): Option[MetricsSummary] = {
    val maybeSummaryData = store.getMetricsSummary()
    val summary = maybeSummaryData.map(allDocuments => {
      val totalCalls = allDocuments.size
      val totalParamsFiltered = allDocuments.map(doc => {
        val totalParamCount = doc.getData.get(store.totalParamsCountKey).toString
        val totalSafeCount = doc.getData.get(store.safeParamsCountKey).toString
        val diff = totalParamCount.toInt - totalSafeCount.toInt
        diff
      }).sum
      MetricsSummary(totalCalls, totalParamsFiltered)
    })
    summary
  }

  private def getUriObj[T](requestUri: String): Option[URI] = {
    try {
      var uri = requestUri
      // If starts with space, just remove it
      if(uri.startsWith("%20")) {
        uri = uri.replaceFirst("%20", "")
      }
      if (!uri.startsWith("http") && !uri.startsWith("https")) {
        uri = s"http://${uri}"
      }
      val res = new URI(uri)
      val maybeHost = Option(res.getHost)
      maybeHost.map(_ => res)
    } catch {
      case _: Exception => None
    }
  }

  private def addMetrics(host: String, totalParamsCount: Int, safeParamsCount: Int = 0): Future[Unit] = {
    Future {
      store.createMetrics(host, totalParamsCount, safeParamsCount)
    }
  }

  private def refRemoverWithRuleEngine(uriObj: URI,
                                       safeParamsList: List[String],
                                       removeRefFromUriEnd: Boolean = false,
                                       redirectParams: List[String] = List.empty[String]): RemoveParamRes = {
    val queryParamsMap = getQueryParamsMap(uriObj)
    val maybeRedirectParam = queryParamsMap.find(p => redirectParams.contains(p._1))
    if(maybeRedirectParam.isEmpty) {
      val (newUri, safeParamsSize) = if (queryParamsMap.nonEmpty) {
        val uriWithNoParams = getUriWithNoParam(uriObj, removeRefFromUriEnd)
        // Add filtered Params
        val safeParams = queryParamsMap.filter {
          case (k, _) => safeParamsList.contains(k)
        }
        val safeParamsStr = safeParams.map {
          case (k, v) => s"${k}=${v}"
        }.mkString("&")
        val newUri = s"${uriWithNoParams}?${safeParamsStr}"
        (newUri, safeParams.size)
      } else {
        // If no query params, no need to filter anything. Just call the requested URI
        val newUri = uriObj.toString
        (newUri, 0)
      }
      RemoveParamRes(newUri, uriObj.getHost, queryParamsMap.size, safeParamsSize)
    } else {
      val newUriObj = new URI(maybeRedirectParam.get._2)
      refRemoverWithRuleEngine(newUriObj, safeParamsList, removeRefFromUriEnd, List.empty)
    }
  }

  private def getUriWithNoParam(uriObj: URI, removeRefFromUriEnd: Boolean = false): String = {
    val rawQuery = Option(uriObj.getRawQuery)
    var uriString = uriObj.toString.replace("?", "")
    if(removeRefFromUriEnd) {
      val indexofRef = uriString.indexOf("ref=")
      uriString = uriString.substring(0, indexofRef)
    }
    rawQuery.map(uriString.replace(_, "")).getOrElse(uriString)}

  private def getQueryParamsMap(uriObj: URI): Map[String, String] = {
    val queryParamsString = Option(uriObj.getRawQuery)
    queryParamsString.map(_.split("&").map(v => {
      val m = v.split("=", 2).map(s => URLDecoder.decode(s, "UTF-8"))
      m(0) -> (if(m.size > 1) m(1) else "")
    }).toMap
    ).getOrElse(Map.empty[String, String])
  }

}
