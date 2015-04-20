package com.github.vergenzt.rtmscala

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.xml.NodeSeq

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime

import scalaj.http.Http
import scalaj.http.HttpRequest
import util._
import util.ParamConversions._
import util.XmlConversions._

object rtm extends Rtm

/**
 * Scala wrapper for the Remember the Milk API.
 *
 * @define authToken @param authToken An authentication token.
 * @define creds @param creds Your API credentials.
 * @define frob @param frob A frob obtained from `rtm.auth.getFrob`
 * @define method @param method The API method to call, without the prefix "rtm."
 *   (e.g."rtm.auth.getFrob" would be passed as "auth.getFrob")
 * @define params @param params The method-specific parameters.
 * @define perms @param perms The requested permission.
 * @define timeline @param timeline A timeline obtained from `rtm.timelines.create`
 */
class Rtm {

  val BASE_URL = "http://api.rememberthemilk.com/services"
  val AUTH_URL = BASE_URL + "/auth/"
  val REST_URL = BASE_URL + "/rest/"

  /**
   * Construct an RTM API request.
   * @param url The url to request.
   * $params
   * $creds
   */
  protected[rtmscala]
  def baseRequest(url: String, params: (String, String)*)
      (implicit creds: ApiCreds): HttpRequest = {
    Http(url).param("api_key", creds.apiKey).params(params)
  }

  /**
   * Get an unsigned RTM API request.
   * $method
   * $params
   * $creds
   */
  protected[rtmscala]
  def unsignedRequest(method: String, params: (String, String)*)
      (implicit creds: ApiCreds): HttpRequest = {
    baseRequest(REST_URL, ("method" -> ("rtm." + method)) :: params.toList: _*)
  }

  /**
   * Get a signed RTM API request.
   * $method
   * $params
   * $creds
   */
  protected[rtmscala]
  def request(method: String, params: (String, String)*)
      (implicit creds: ApiCreds): HttpRequest = {
    unsignedRequest(method, params: _*).signed
  }

  /**
   * Construct an authenticated RTM API request.
   * $method
   * $params
   * $creds
   * $authToken
   */
  protected[rtmscala]
  def authedRequest(method: String, params: (String, String)*)
      (implicit creds: ApiCreds, authToken: AuthToken) = {
    request(method, ("auth_token" -> authToken.token) :: params.toList: _*)
  }

  /**
   * Construct an authenticated RTM API request with a timeline.
   * $method
   * $params
   * $creds
   * $authToken
   * $timeline
   */
  protected[rtmscala]
  def timelinedRequest(method: String, params: (String, String)*)
      (implicit creds: ApiCreds, authToken: AuthToken, timeline: Timeline) = {
    authedRequest(method, ("timeline" -> timeline.id) :: params.toList: _*)
  }

  /*****************************
   * Begin API implementation. *
   *****************************/

  val auth = new Auth
  class Auth {
    /**
     * Get a Remember the Milk authentication URL to direct a user to.
     * $perms
     * $creds
     */
    def getURL(perms: Permission)(implicit creds: ApiCreds) =
      baseRequest(AUTH_URL, perms).signed.fullURL

    /**
     * Get a Remember the Milk authentication URL to direct a user to.
     * $perms
     * $frob
     * $creds
     */
    def getURL(perms: Permission, frob: Frob)(implicit creds: ApiCreds) =
      baseRequest(AUTH_URL, perms, frob).signed.fullURL

    /**
     * Authenticate the user with Remember the Milk.
     * $perms
     * @param directUserToURL A function to direct the user to the generated
     *   authentication URL. Should return a Future that completes once the
     *   user has finished authenticating.
     * $creds
     * @param executionContext The context in which to execute the Future.
     * @return A Future[AuthToken] that completes once the user has
     *   authenticated and the resulting token has been fetched.
     */
    def authenticate(perms: Permission)(directUserToURL: String => Future[Unit])
      (implicit creds: ApiCreds, executionContext: ExecutionContext): Future[AuthToken] = {

      assert (perms.name != "none")
      val frob = getFrob()
      directUserToURL(getURL(perms, frob)).map(_ => auth.getToken(frob))
    }

    def checkToken(token: AuthToken)(implicit creds: ApiCreds) =
      request("auth.checkToken", token).as[AuthToken]

    def getFrob()(implicit creds: ApiCreds) =
      request("auth.getFrob").as[Frob]

    def getToken(frob: Frob)(implicit creds: ApiCreds) =
      request("auth.getToken", frob).as[AuthToken]
  }

  val contacts = new Contacts
  class Contacts {
    def add(usernameOrEmail: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("contacts.add", "contact" -> usernameOrEmail).as[Contact]

    def delete(contact: Contact)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("contacts.delete", contact).as[Unit]

    def getList()(implicit creds: ApiCreds, token: AuthToken): Unit =
      authedRequest("contacts.getList").as[Seq[Contact]]
  }

  val groups = new Groups
  class Groups {
    def add(name: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("groups.add", "group" -> name).as[Group]

    def addContact(group: Group, contact: Contact)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("groups.addContact", group, contact).as[Unit]

    def delete(group: Group)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("groups.delete", group).as[Unit]

    def getList()(implicit creds: ApiCreds, token: AuthToken): Unit =
      authedRequest("groups.getList").as[Seq[Group]]

    def removeContact(group: Group, contact: Contact)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("groups.removeContact", group, contact).as[Unit]
  }

  val lists = new Lists
  class Lists {
    def add(name: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("lists.add", "name" -> name).as[List]

    def add(name: String, filter: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("lists.add", "name" -> name, "filter" -> filter).as[List]

    def archive(list: List)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("lists.archive", list).as[List]

    def delete(list: List)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("lists.delete", list).as[List]

    def getList()(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("lists.getList").as[Seq[List]]

    def setDefaultList(list: List)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("lists.setDefaultList", list).as[Unit]

    def setName(list: List, newName: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("lists.setName", list).as[List]

    def unarchive(list: List)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("lists.unarchive", list).as[List]
  }

  val locations = new Locations
  class Locations {
    def getList()(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("locations.getList").as[Seq[Location]]
  }

  val reflection = new Reflection
  class Reflection {
    // TODO: getMethodInfo
    // TODO: getMethods
  }

  val settings = new Settings
  class Settings {
    // TODO: getList
  }

  val tasks = new Tasks
  class Tasks {
    def add(name: String, list: List, parse: Boolean)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.add", list, "parse" -> parse).as[Task]

    def add(name: String, parse: Boolean = true)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.add", "parse" -> parse).as[Task]

    def addTags(task: Task, tags: Seq[String])
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.addTags", ("tags" -> tags.mkString(",")) :: task.toList: _*).as[Task]

    def complete(task: Task)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.complete", task: _*).as[Task]

    def delete(task: Task)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.delete", task: _*).as[Unit]

    def getList()(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("tasks.getList").as[Seq[Task]]

    def getList(since: DateTime)(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("tasks.getList", "last_sync" -> since.toString(util.rtmDateTimeFormat)).as[Seq[Task]]

    def getList(filter: String)(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("tasks.getList", "filter" -> filter).as[Seq[Task]]

    def getList(list: List, filter: String)(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("tasks.getList", list, "filter" -> filter).as[Seq[Task]]

    def movePriority(task: Task, direction: String)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) = {
      require(direction == "up" || direction == "down")
      timelinedRequest("tasks.addTags", ("direction" -> direction) :: task.toList: _*).as[Task]
    }

    def moveTo(task: Task, list: List)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) = {
      timelinedRequest("tasks.moveTo",
        "task_id" -> task.id, "taskseries_id" -> task.seriesId,
        "from_list_id" -> task.listId,
        "to_list_id" -> list.id
      ).as[Task]

    }

    def postpone(task: Task)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.postpone", task: _*).as[Task]

    def removeTags(task: Task, tags: Seq[String])
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.removeTags",
        ("tags" -> tags.mkString(",")) ::
        task.toList: _*
      ).as[Task]

    def setDueDate(task: Task, due: String)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setDueDate", ("parse" -> "1") :: ("due" -> due) :: task.toList: _*).as[Task]

    def setDueDate(task: Task, due: LocalDate)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setDueDate",
        ("due" -> due.toString(rtmDateTimeFormat)) ::
        task.toList: _*
      ).as[Task]

    def setDueDate(task: Task, due: LocalDateTime)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setDueDate",
        ("due" -> due.toString(rtmDateTimeFormat)) ::
        ("has_due_time" -> "1") ::
        task.toList: _*
      ).as[Task]

    def unsetDueDate(task: Task)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setDueDate", task: _*).as[Task]

    def setEstimate(task: Task, estimate: Option[String])
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setEstimate",
        ("estimate" -> estimate.getOrElse("")) ::
        task.toList: _*
      ).as[Task]

    def setLocation(task: Task, location: Option[Location])
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setEstimate",
        ("location_id" -> location.map(_.id).getOrElse("")) ::
        task.toList: _*
      ).as[Task]

    def setName(task: Task, newName: String)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setName",
        ("name" -> newName) ::
        task.toList: _*
      ).as[Task]

    def setPriority(task: Task, newPriority: Int)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) = {
      require (0 <= newPriority && newPriority <= 4)
      timelinedRequest("tasks.setName",
        ("name" -> newPriority.toString) ::
        task.toList: _*
      ).as[Task]
    }

    def setRecurrence(task: Task, recurrence: Option[String])
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setName",
        ("repeat" -> recurrence.getOrElse("")) ::
        task.toList: _*
      ).as[Task]

    def setTags(task: Task, tags: Seq[String])
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setTags",
        ("tags" -> tags.mkString(",")) ::
        task.toList: _*
      ).as[Task]

    def setURL(task: Task, url: Option[String])
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.setName",
        ("url" -> url.getOrElse("")) ::
        task.toList: _*
      ).as[Task]

    def uncomplete(task: Task)
        (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.uncomplete", task: _*).as[Task]

    val notes = new Notes
    class Notes {
      def add(task: Task, title: String, body: String)
          (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
        timelinedRequest("tasks.notes.add",
          ("note_title" -> title) ::
          ("note_text" -> body) ::
          task.toList: _*
        ).as[Note]

      def delete(note: Note)
          (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
        timelinedRequest("tasks.notes.delete", note).as[Note]

      def edit(note: Note, newTitle: String, newBody: String)
          (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
        timelinedRequest("tasks.notes.edit",
          note, "note_title" -> newTitle, "note_text" -> newBody
        ).as[Note]
    }
  }

  val test = new Test
  class Test {
    def echo(params: (String, String)*)(implicit creds: ApiCreds) =
      unsignedRequest("test.echo", params: _*).asString.body

    def login(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("test.login").as[AuthToken]
  }

  val time = new Time
  class Time {
    // TODO: convert
    // TODO: parse
  }

  val timelines = new Timelines
  class Timelines {
    def create(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("timelines.create").as[Timeline]
  }

  val timezones = new Timezones
  class Timezones {
    // TODO: getList
  }

  val transactions = new Transactions
  class Transactions {
    def undo(transaction: Transaction)(implicit creds: ApiCreds, token: AuthToken) = {
      authedRequest("transactions.undo", transaction.timeline, transaction).as[Unit]
      // remove the transaction from the timeline
      transaction.timeline._transactions -= transaction
    }
  }
}
