package com.github.vergenzt.rtmscala

import org.joda.time.DateTime
import scala.xml.NodeSeq
import scala.xml.Node
import scala.collection.mutable
import scalaj.http.HttpRequest

/* Authentication */

case class ApiCreds(apiKey: String, secret: String)

case class AuthToken(token: String, perms: Permission, user: User)

case class Frob(frob: String)

sealed class Permission(val name: String, val value: Int) extends Ordered[Permission] {
  def compare(that: Permission) = this.value - that.value
}

object Permission {
  case object None extends Permission("none", 0)
  case object Read extends Permission("read", 1)
  case object Write extends Permission("write", 2)
  case object Delete extends Permission("delete", 3)
}

case class User(id: String, username: String, fullname: Option[String])

/* Main classes */

case class Contact(
  id: String,
  username: String,
  fullname: String
)

case class Group(
  id: String,
  name: String,
  contactIds: Seq[String]
)

case class List(
  id: String,
  name: String,
  deleted: Boolean,
  locked: Boolean,
  archived: Boolean,
  position: Int,
  filter: Option[String]
)

case class Location(
  id: String,
  name: String,
  latitude: Double,
  longitude: Double,
  zoom: Int,
  address: String,
  viewable: Boolean
)

case class Note(
  id: String,
  title: String,
  text: String,
  created: DateTime,
  modified: DateTime
)

// TODO: explore how this really works
case class RepetitionRule(
  desc: String,
  every: Boolean
)

case class Task(
  // ids
  id: String,
  seriesId: String,
  listId: String,
  // main data
  name: String,
  due: Option[DateTime],
  hasDueTime: Boolean,
  tags: Seq[String],
  repetition: Option[RepetitionRule],
  url: Option[String],
  priority: Int,
  postponed: Int,
  estimate: Option[String],
  source: String,
  notes: Seq[Note],
  participants: Seq[String], // TODO: figure out what this is
  // metadata
  created: DateTime,
  modified: DateTime,
  added: DateTime,
  completed: Option[DateTime],
  deleted: Option[DateTime]
) {
  require (0 <= priority && priority <= 4)
}

/* Timelines and Transactions */

case class Timeline(id: String) {
  protected[rtmscala] val _transactions = mutable.Buffer[Transaction]()

  def transactions: Seq[Transaction] = _transactions
}

case class Transaction(
  timeline: Timeline,
  id: String,
  undoable: Boolean,
  request: HttpRequest
)

/* Exceptions */

case class RtmException(val message: String, val code: Int) extends Exception {
  override def getMessage = message
}
