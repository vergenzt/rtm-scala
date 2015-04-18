package com.github.vergenzt.rtmscala.util

import scala.xml.NodeSeq

import com.github.vergenzt.rtmscala._

object XmlConversions {

  implicit def xml2AuthToken(xml: NodeSeq): AuthToken = xml match {
    case Seq(
        <auth><token>{token}</token><perms>{perms}</perms>{user @ <user/>}</auth>
      ) =>
      AuthToken(
        token.text,
        Permission(perms.text),
        xml2User(user)
      )
  }

  implicit def xml2Frob(xml: NodeSeq): Frob = xml match {
    case Seq(<frob>{frob}</frob>) =>
      Frob(frob.text)
  }

  implicit def xml2List(xml: NodeSeq): List = xml match {
    case Seq(list @ (<list/> | <list><filter/></list>)) => List(
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
    case Seq(<lists>{lists @ _*}</lists>) => lists.map(xml2List)
    case Seq(<lists/>) => Seq()
  }

  implicit def xml2Timeline(xml: NodeSeq): Timeline = xml match {
    case Seq(<timeline>{id}</timeline>) => Timeline(id.text)
  }

  implicit def xml2TaskSeq(xml: NodeSeq): Seq[Task] =
    for {
      list @ <list>{contents @ _*}</list> <- xml
      taskseries <- contents \\ "taskseries"
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

  implicit def xml2Note(xml: NodeSeq): Note = xml match {
    case Seq(note @ <note>{body}</note>) => Note(
      note \@ "id",
      note \@ "title",
      body.text,
      parseDateTime(note \@ "created"),
      parseDateTime(note \@ "modified")
    )
  }

  implicit def xml2NoteSeq(xml: NodeSeq): Seq[Note] = xml match {
    case Seq(<notes>{notes @ _*}</notes>) => notes.map(xml2Note)
    case Seq(<notes/>) => Seq()
  }

  implicit def xml2User(xml: NodeSeq): User = xml match {
    case Seq(user @ <user><username>{username}</username></user>) =>
      User(user \@ "id", username.text, None)
    case Seq(user @ <user/>) =>
      User(user \@ "id", user \@ "username", Some(user \@ "fullname"))
  }

  implicit def xml2Location(xml: NodeSeq): Location = xml match {
    case Seq(location @ <location/>) => Location(
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

  implicit def xml2TransactionWith[T](xml: NodeSeq)(implicit xml2T: NodeSeq => T): (Transaction, T) =
    xml match {
      case Seq(transaction @ <transaction/>, rest) => (
        Transaction(transaction \@ "id", transaction \@ "undoable"),
        xml2T(rest)
      )
    }
}
