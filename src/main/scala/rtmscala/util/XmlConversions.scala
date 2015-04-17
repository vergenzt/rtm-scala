package rtmscala.util

import scala.xml.NodeSeq
import rtmscala._

object XmlConversions {

  implicit def xml2AuthToken(xml: NodeSeq) = {

    val head = xml.head
  head match {
    case <auth><token>{token}</token><perms>{perms}</perms>{user @ <user/>}</auth> =>
      AuthToken(
        token.text,
        Permission(perms.text),
        xml2User(user)
      )
  }
  }

  implicit def xml2Frob(xml: NodeSeq) = xml.head match {
    case Seq(<frob>{frob}</frob>) =>
      Frob(frob.text)
  }

  implicit def xml2List(xml: NodeSeq) = xml.head match {
    case list @ (<list/> | <list><filter/></list>) => List(
      list \@ "id",
      list \@ "name",
      list \@ "deleted",
      list \@ "locked",
      list \@ "archived",
      (list \@ "position").toInt,
      if (list \@ "smart") Some((list \ "filter").text) else None
    )
  }

  implicit def xml2Timeline(xml: NodeSeq) = xml.head match {
    case <timeline>{id}</timeline> => Timeline(id.text)
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

  implicit def xml2Note(xml: NodeSeq): Note = xml.head match {
    case note @ <note>{body}</note> => Note(
      note \@ "id",
      note \@ "title",
      body.text,
      parseDateTime(note \@ "created"),
      parseDateTime(note \@ "modified")
    )
  }

  implicit def xml2User(xml: NodeSeq): User = xml.head match {
    case user @ <user><username>{username}</username></user> =>
      User(user \@ "id", username.text, None)
    case user @ <user/> =>
      User(user \@ "id", user \@ "username", Some(user \@ "fullname"))
  }
}
