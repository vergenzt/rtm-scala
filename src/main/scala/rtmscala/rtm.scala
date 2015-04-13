package rtmscala

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import scalaj.http.Http
import scalaj.http.HttpRequest

object rtm {
  import util._
  import OptionConversions._
  import ParamConversions._
  import XmlConversions._

  val AUTH_URL = "http://api.rememberthemilk.com/services/auth/"
  val REST_URL = "http://api.rememberthemilk.com/services/rest/"

  /**
   * Construct a Remember the Milk API request. Call `.as[Type]` with
   * a domain object type to fetch the request and convert to the domain
   * object.
   *
   * @param method the method to be called, minus the prefix "rtm."
   * @param needsSignature true if the request should be signed
   * @param params the remaining method-specific parameters
   * @param creds your API credentials
   * @param authTokenOption the AuthToken, if available. There is an
   *   implicit conversion available to convert an AuthToken to an
   *   Option[AuthToken]. See `rtm.auth.authenticate`.
   * @param timelineOption the Timeline, if available. There is an
   *   implicit conversion available to convert a Timeline to an
   *   Option[Timeline]. See `rtm.timelines.create`.
   */
  def request(method: String, needsSignature: Boolean = true)
    (params: (String,String)*)
    (implicit
      creds: ApiCreds,
      authTokenOption: Option[AuthToken],
      timelineOption: Option[Timeline]
    ): HttpRequest = {

    val url = if (method == "auth") AUTH_URL else REST_URL

    // api key
    var request = Http(url).params("api_key" -> creds.apiKey)
    // method
    if (method != "auth") request = request.param("method", "rtm." + method)
    // auth_token
    authTokenOption.foreach(authToken => request = request.params(authToken))
    // timeline
    timelineOption.foreach(timeline => request = request.params(timeline))
    // <other params>
    request = request.params(params)
    // api_sig
    if (needsSignature) request = request.signed
    // <return result>
    request
  }

  /*****************************
   * Begin API implementation. *
   *****************************/

  object auth {
    /**
     * Get a Remember the Milk authentication URL to direct a user to.
     * @param perms the requested permission
     * @param frob an optional frob parameter obtained from `rtm.auth.getFrob`
     */
    def getURL(perms: Permission, frob: Option[Frob] = None)(implicit creds: ApiCreds) = frob match {
      case Some(frob) => request("auth")(perms, frob).fullURL
      case None       => request("auth")(perms).fullURL
    }

    /**
     * Authenticate the user with Remember the Milk.
     * @param perms the requested permission
     * @param directUserToURL a function to direct the user to the generated
     *   authentication URL. Should return a Future that completes once the
     *   user has finished authenticating.
     * @return a Future[AuthToken] that completes once the user has
     *   authenticated and the resulting token has been fetched
     */
    def authenticate(perms: Permission, directUserToURL: String => Future[Unit])
      (implicit creds: ApiCreds, executionContext: ExecutionContext): Future[AuthToken] = {

      assert (perms.name != "none")
      val frob = auth.getFrob
      val url = getURL(perms, Some(frob))
      directUserToURL(url).map(_ => auth.getToken(frob))
    }

    def checkToken(implicit creds: ApiCreds, token: AuthToken) =
      request("auth.checkToken")().as[AuthToken]

    def getFrob(implicit creds: ApiCreds) =
      request("auth.getFrob")().as[Frob]

    def getToken(frob: Frob)(implicit creds: ApiCreds) =
      request("auth.getToken")(frob).as[AuthToken]
  }

  object contacts {
    // TODO: add
    // TODO: delete
    // TODO: getList
  }

  object groups {
    // TODO: add
    // TODO: addContact
    // TODO: delete
    // TODO: getList
    // TODO: removeContact
  }

  object lists {
    def add(name: String, filter: Option[String])(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      filter match {
        case Some(filter) => request("lists.add")("name" -> name, "filter" -> filter).as[List]
        case None         => request("lists.add")("name" -> name).as[List]
      }

    // TODO: archive
    // TODO: delete

    def getList(implicit creds: ApiCreds, token: AuthToken) =
      request("lists.getList")().as {
        case <lists>{lists @ _*}</lists> => lists.map(xml2List)
      }

    // TODO: setDefaultList
    // TODO: setName
    // TODO: unarchive
  }

  object locations {
    // TODO: getList
  }

  object reflection {
    // TODO: getMethodInfo
    // TODO: getMethods
  }

  object settings {
    // TODO: getList
  }

  object tasks {
    def add(name: String, parse: Boolean = false, list: Option[List] = None)
      (implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      list match {
        case Some(list) =>
          request("tasks.add")("name" -> name, "parse" -> parse, list).as[Seq[Task]]
        case None =>
          request("tasks.add")("name" -> name, "parse" -> parse).as[Seq[Task]]
      }

    // TODO: addTags

    def complete(task: Task)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      request("tasks.add")(task: _*).as[Seq[Task]].head

    def delete(task: Task)(implicit creds: ApiCreds, token: AuthToken, timeline: Timeline) =
      request("tasks.delete")(task: _*).as[Seq[Task]].head

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

    object notes {
      // TODO: add
      // TODO: delete
      // TODO: edit
    }
  }

  object test {
    def echo(implicit creds: ApiCreds) =
      request("test.echo", needsSignature=false)().as(_.toString)
    def login(implicit creds: ApiCreds, token: AuthToken) =
      request("test.login")().as[User]
  }

  object time {
    // TODO: convert
    // TODO: parse
  }

  object timelines {
    def create(implicit creds: ApiCreds, token: AuthToken) =
      request("timelines.create")().as[Timeline]
  }

  object timezones {
    // TODO: getList
  }

  object transactions {
    // TODO: undo
  }
}
