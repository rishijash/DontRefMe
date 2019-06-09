package models

case class RemoveParamRes (
  newUri: String,
  host: String,
  totalParams: Int,
  filterdParams: Int
)
