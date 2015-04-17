package com.github.vergenzt.rtmscala

import org.joda.time.DateTime

/* Authentication */

case class ApiCreds(apiKey: String, secret: String)

case class Frob(frob: String)

case class Permission(name: String) extends Ordered[Permission] {
  private val options = Seq("none", "read", "write", "delete")
  require (options contains name)
  def value = options.indexOf(name)
  def compare(that: Permission) = this.value - that.value
}

case class AuthToken(token: String, perms: Permission, user: User)

case class User(id: String, username: String, fullname: Option[String])

/* Tasks and lists */

case class List(
  id: String,
  name: String,
  deleted: Boolean,
  locked: Boolean,
  archived: Boolean,
  position: Int,
  filter: Option[String]
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

case class Note(
  id: String,
  title: String,
  text: String,
  created: DateTime,
  modified: DateTime
)

/* Contacts and Groups */

case class Contact(
  id: String,
  username: String,
  fullname: String
)

case class Group(id: String, name: String, contactIds: Seq[String])

/* Timelines */

case class Timeline(id: String)

/* Exceptions */

case class RtmException(val message: String, val code: Int) extends Exception {
  override def getMessage = message
}
