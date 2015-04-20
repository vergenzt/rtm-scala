package com.github.vergenzt.rtmscala.util

import scala.xml.NodeSeq
import com.github.vergenzt.rtmscala._
import scala.xml.Node
import scala.xml.Text

object XmlConversions {

  type XmlParser[T] = (NodeSeq => T)

  import rtmDateTimeFormat.parseDateTime

  implicit def xml2Unit(xml: NodeSeq): Unit = {}

  /* Authentication */

  implicit def xml2AuthToken(xml: NodeSeq): AuthToken = xml match {
    case Seq(auth @ Node("auth", _, token, perms, user)) =>
      AuthToken(
        token.text,
        xml2Perms(perms),
        xml2User(user)
      )
  }

  implicit def xml2Frob(xml: NodeSeq): Frob = xml match {
    case Seq(Node("frob", _, Text(frob))) =>
      Frob(frob)
  }

  implicit def xml2Perms(xml: NodeSeq): Permission = xml match {
    case Seq(Node("perms", _, Text(perms))) => perms match {
      case "read" => Permission.Read
      case "write" => Permission.Write
      case "delete" => Permission.Delete
    }
  }

  implicit def xml2User(xml: NodeSeq): User = xml match {
    case Seq(user @ Node("user", attr, _*)) => User(
      user \@ "id",
      Some(user \@ "username").filterNot(_ == "").getOrElse((user \ "username").text),
      Some(user \@ "fullname").filterNot(_ == "")
    )
  }

  /* Main classes */

  implicit def xml2Contact(xml: NodeSeq): Contact = xml match {
    case Seq(contact @ <contact/>) => Contact(
      contact \@ "id",
      contact \@ "username",
      contact \@ "fullname"
    )
  }

  implicit def xml2ContactSeq(xml: NodeSeq): Seq[Contact] = xml match {
    case Seq(<contacts>{contacts @ _*}</contacts>) => contacts.map(xml2Contact)
    case Seq(<contacts/>) => Seq()
  }

  implicit def xml2Group(xml: NodeSeq): Group = xml match {
    case Seq(group @ <group>{contacts}</group>) => Group(
      group \@ "id",
      group \@ "name",
      (contacts \ "contact").map(contact => contact \@ "id")
    )
  }

  implicit def xml2GroupSeq(xml: NodeSeq): Seq[Group] = xml match {
    case Seq(<groups>{groups @ _*}</groups>) => groups.map(xml2Group)
  }

  implicit def xml2List(xml: NodeSeq): List = xml match {
    case Seq(list @ Node("list", _, _*)) => List(
      list \@ "id",
      list \@ "name",
      list \@ "deleted",
      list \@ "locked",
      list \@ "archived",
      (list \@ "position").toInt,
      if (list \@ "smart") Some((list \ "filter").text) else None
    )
  }

  implicit def xml2ListSeq(xml: NodeSeq): Seq[List] = xml match {
    case Seq(Node("lists", _, lists @ _*)) => lists.map(xml2List)
  }

  implicit def xml2Location(xml: NodeSeq): Location = xml match {
    case Seq(location @ Node("location", _, _*)) => Location(
      location \@ "id",
      location \@ "name",
      (location \@ "latitude").toDouble,
      (location \@ "longitude").toDouble,
      (location \@ "zoom").toInt,
      location \@ "address",
      location \@ "viewable"
    )
  }

  implicit def xml2LocationSeq(xml: NodeSeq): Seq[Location] = xml match {
    case Seq(<locations>{locations @ _*}</locations>) => locations.map(xml2Location)
  }

  implicit def xml2Note(xml: NodeSeq): Note = xml match {
    case Seq(note @ Node("note", _, body @ _*)) => Note(
      note \@ "id",
      note \@ "title",
      body.text,
      parseDateTime(note \@ "created"),
      parseDateTime(note \@ "modified")
    )
  }

  implicit def xml2NoteSeq(xml: NodeSeq): Seq[Note] = xml match {
    case Seq(Node("notes", _, notes)) => notes.map(xml2Note)
  }

  implicit def xml2Task(xml: NodeSeq): Task = xml2TaskSeq(xml) match {
    case Seq(task) => task
  }

  implicit def xml2TaskSeq(xml: NodeSeq): Seq[Task] =
    for {
      list <- xml \ "list"
      taskseries <- list \ "taskseries"
      task <- taskseries \ "task"
    } yield {
      Task(
        id = task \@ "id",
        seriesId = taskseries \@ "id",
        listId = list \@ "id",
        name = taskseries \@ "name",
        due = Some(task \@ "due").filterNot(_ == "").map(parseDateTime),
        hasDueTime = task \@ "has_due_time",
        tags = (taskseries \ "tags" \\ "tag").map(_.text),
        repetition = (taskseries \ "rrule").headOption.map(rrule => RepetitionRule(rrule.text, rrule \@ "every")),
        url = Some(taskseries \@ "url").filterNot(_ == ""),
        priority = (task \@ "priority") match {
          case "N" => 0
          case p => p.toInt
        },
        postponed = (task \@ "postponed").toInt,
        estimate = Some(task \@ "estimate").filterNot(_ == ""),
        source = taskseries \@ "source",
        notes = (taskseries \ "notes" \ "note").map(xml2Note),
        participants = (taskseries \ "participants").flatMap(_.child).map(_.text),
        created = parseDateTime(taskseries \@ "created"),
        modified = parseDateTime(taskseries \@ "modified"),
        added = parseDateTime(task \@ "added"),
        completed = Some(task \@ "completed").filterNot(_ == "").map(parseDateTime),
        deleted = Some(task \@ "deleted").filterNot(_ == "").map(parseDateTime)
      )
    }

  /* Timelines and transactions */

  implicit def xml2Timeline(xml: NodeSeq): Timeline = xml match {
    case Seq(Node("timeline", _, id)) => Timeline(id.text)
  }
}
