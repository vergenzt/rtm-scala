package com.github.vergenzt.rtmscala

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import scalaj.http.Http
import scalaj.http.HttpRequest

import util._
import util.ParamConversions._
import util.XmlConversions._

object rtm extends Rtm

/**
 * Scala wrapper for the Remember the Milk API.
 *
 * @define authToken An authentication token.
 * @define creds Your API credentials.
 * @define frob A frob obtained from `rtm.auth.getFrob`
 * @define method The API method to call, without the prefix "rtm."
 *   (e.g."rtm.auth.getFrob" would be passed as "auth.getFrob")
 * @define params The method-specific parameters.
 * @define perms The requested permission.
 * @define timeline A timeline obtained from `rtm.timelines.create`
 */
class Rtm {

  val BASE_URL = "http://api.rememberthemilk.com/services"
  val AUTH_URL = BASE_URL + "/auth/"
  val REST_URL = BASE_URL + "/rest/"

  /**
   * Construct a Remember the Milk API request.   *
   *
   * @param url The url to request.
   * @param params The method-specific parameters.
   * @param creds $creds
   */
  def baseRequest(url: String, params: (String, String)*)(implicit creds: ApiCreds): HttpRequest = {
    Http(url).param("api_key", creds.apiKey).params(params)
  }

  /**
   * Construct an unsigned RTM API request.
   * @param method $method
   * @param params $params
   * @param creds $creds
   */
  def unsignedRequest(method: String, params: (String, String)*)(implicit creds: ApiCreds) = {
    baseRequest(REST_URL, ("method" -> ("rtm." + method)) :: params.toList: _*)
  }

  /**
   * Construct a signed RTM API request.
   * @param method $method
   * @param params $params
   * @param creds $creds
   */
  def request(method: String, params: (String, String)*)(implicit creds: ApiCreds) = {
    unsignedRequest(method, params: _*).signed
  }

  /**
   * Construct an authenticated RTM API request.
   * @param method $method
   * @param params $params
   * @param creds $creds
   * @param authToken $authToken
   */
  def authedRequest(method: String, params: (String, String)*)(implicit creds: ApiCreds, authToken: AuthToken) = {
    request(method, ("auth_token" -> authToken.token) :: params.toList: _*)
  }

  /**
   * Construct an authenticated RTM API request with a timeline.
   * @param method $method
   * @param params $params
   * @param creds $creds
   * @param authToken $authToken
   * @param timeline $timeline
   */
  def timelinedRequest(method: String, params: (String, String)*)(
      implicit creds: ApiCreds, authToken: AuthToken, timeline: Timeline) = {
    authedRequest(method, ("timeline" -> timeline.id) :: params.toList: _*)
  }

  /*****************************
   * Begin API implementation. *
   *****************************/

  val auth = new Auth
  class Auth {
    /**
     * Get a Remember the Milk authentication URL to direct a user to.
     * @param perms $perms
     * @param creds $creds
     */
    def getURL(perms: Permission)(implicit creds: ApiCreds) =
      baseRequest(AUTH_URL, perms).signed.fullURL

    /**
     * Get a Remember the Milk authentication URL to direct a user to.
     * @param perms $perms
     * @param frob $frob
     * @param creds $creds
     */
    def getURL(perms: Permission, frob: Frob)(implicit creds: ApiCreds) =
      baseRequest(AUTH_URL, perms, frob).signed.fullURL

    /**
     * Authenticate the user with Remember the Milk.
     * @param perms $perms
     * @param directUserToURL A function to direct the user to the generated
     *   authentication URL. Should return a Future that completes once the
     *   user has finished authenticating.
     * @param creds $creds
     * @param executionContext The context in which to execute the Future.
     * @return A Future[AuthToken] that completes once the user has
     *   authenticated and the resulting token has been fetched.
     */
    def authenticate(perms: Permission, directUserToURL: String => Future[Unit])
      (implicit creds: ApiCreds, executionContext: ExecutionContext): Future[AuthToken] = {

      assert (perms.name != "none")
      val frob = getFrob
      directUserToURL(getURL(perms, frob)).map(_ => auth.getToken(frob))
    }

    def checkToken(implicit creds: ApiCreds, token: AuthToken) = authedRequest("auth.checkToken").as[AuthToken]
    def getFrob(implicit creds: ApiCreds)                      = request("auth.getFrob").as[Frob]
    def getToken(frob: Frob)(implicit creds: ApiCreds)         = request("auth.getToken", frob).as[AuthToken]
  }

  val contacts = new Contacts
  class Contacts {
    def add(usernameOrEmail: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("contacts.add", "contact" -> usernameOrEmail).as[Contact]

    def delete(contact: Contact)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("contacts.delete", contact).as { _ => }

    def getList(implicit creds: ApiCreds, token: AuthToken): Unit =
      authedRequest("contacts.getList").as[Seq[Contact]]
  }

  val groups = new Groups
  class Groups {
    def add(name: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("groups.add", "group" -> name).as[Group]

    def addContact(group: Group, contact: Contact)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("groups.addContact", group, contact).as { _ => }

    def delete(group: Group)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("groups.delete", group).as { _ => }

    def getList(implicit creds: ApiCreds, token: AuthToken): Unit =
      authedRequest("groups.getList").as[Seq[Group]]

    def removeContact(group: Group, contact: Contact)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline): Unit =
      timelinedRequest("groups.removeContact", group, contact).as { _ => }
  }

  val lists = new Lists
  class Lists {
    def add(name: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("lists.add", "name" -> name).as[List]

    def add(name: String, filter: String)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("lists.add", "name" -> name, "filter" -> filter).as[List]

    // TODO: archive
    // TODO: delete

    def getList(implicit creds: ApiCreds, token: AuthToken) =
      authedRequest("lists.getList").as {
        case <lists>{lists @ _*}</lists> => lists.map(xml2List)
      }

    // TODO: setDefaultList
    // TODO: setName
    // TODO: unarchive
  }

  val locations = new Locations
  class Locations {
    // TODO: getList
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
    def add(name: String, parse: Boolean = false, listOption: Option[List] = None)
           (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      listOption match {
        case Some(list) => timelinedRequest("tasks.add", "parse" -> parse, list).as[Seq[Task]]
        case None => timelinedRequest("tasks.add", "parse" -> parse).as[Seq[Task]]
      }

    // TODO: addTags

    def complete(task: Task)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.complete", task: _*).as[Seq[Task]].head

    def delete(task: Task)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      timelinedRequest("tasks.delete", task: _*).as[Seq[Task]].head

    // TODO: getList
    // TODO: movePriority
    // TODO: moveTo
    // TODO: postpone
    // TODO: removeTags
    // TODO: setDueDate
    // TODO: setEstimate
    // TODO: setLocation
    // TODO: setName
    // TODO: setPriority
    // TODO: setRecurrence
    // TODO: setTags
    // TODO: setURL
    // TODO: uncomplete

    val notes = new Notes
    class Notes {
      // TODO: add
      // TODO: delete
      // TODO: edit
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
    // TODO: undo
  }
}
