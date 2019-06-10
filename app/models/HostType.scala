package models

object HostType extends Enumeration {

  type HostType = Value
  val Google, Youtube, Amazon, Yahoo = Value

  val commonSafeParams = List("g", "k", "p", "q", "v")

  val hostMap = Map(
    Google -> HostDetails("google.com", List("q", "start")),
    Youtube -> HostDetails("youtube.com", List("search_query", "v")),
    Amazon -> HostDetails("amazon.com", List("k"), removeRefFromStringEnd = true),
    Yahoo -> HostDetails("yahoo.com", List("p"))
  )

  def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)

  def getHostTypeDetailsFromHostUriOpt(hostUri: String): Option[(HostType.Value, HostDetails)] = {
    hostMap.find {
      case (k, v) => hostUri.contains(v.uri)
    }
  }

}
