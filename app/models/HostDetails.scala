package models

case class HostDetails (
  uri: String,
  safeParams: List[String],
  removeRefFromStringEnd: Boolean = false,
  redirectParams: List[String] = List.empty[String]
)
