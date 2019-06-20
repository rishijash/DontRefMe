package models

object HostType extends Enumeration {

  type HostType = Value
  val Google, Youtube, Amazon, Yahoo, Facebook, Instagram, AmazonDe = Value

  val commonSafeParams = List("g", "k", "p", "q", "v")

  val hostMap = Map(
    Google -> HostDetails("google.com", safeParams = List("q", "start")),
    Youtube -> HostDetails("youtube.com", safeParams = List("search_query", "v")),
    Amazon -> HostDetails("amazon.com", safeParams = List("k"), removeRefFromStringEnd = true),
    AmazonDe -> HostDetails("amazon.de", safeParams = List("node", "k"), removeRefFromStringEnd = true),
    Yahoo -> HostDetails("yahoo.com", safeParams = List("p")),
    Instagram -> HostDetails("instagram.com", safeParams = List.empty, redirectParams = List("u")),
    Facebook -> HostDetails("facebook.com", safeParams = List.empty, redirectParams = List("u"))
  )

  def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)

  def getHostTypeDetailsFromHostUriOpt(hostUri: String): Option[(HostType.Value, HostDetails)] = {
    hostMap.find {
      case (k, v) => hostUri.contains(v.uri)
    }
  }

}
