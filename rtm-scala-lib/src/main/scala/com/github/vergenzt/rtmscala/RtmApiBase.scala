package com.github.vergenzt.rtmscala

import scalaj.http.Http
import scalaj.http.HttpRequest
import util._
import util.ParamConversions._
import util.XmlConversions._

object RtmApiBase extends RtmApiBase

/**
 * Base trait for the Remember the Milk API.
 *
 * @define authToken @param authToken An authentication token.
 * @define creds @param creds Your API credentials.
 * @define method @param method The API method to call, without the prefix "rtm."
 *   (e.g."rtm.auth.getFrob" would be passed as "auth.getFrob")
 * @define params @param params The method-specific parameters.
 * @define timeline @param timeline A timeline obtained from `rtm.timelines.create`
 */
trait RtmApiBase {

  val BASE_URL = "http://api.rememberthemilk.com/services"
  val AUTH_URL = BASE_URL + "/auth/"

  // for testing
  private[rtmscala] var REST_URL = BASE_URL + "/rest/"

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
   * Construct an unsigned RTM API request.
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
   * Construct a signed RTM API request.
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
}
