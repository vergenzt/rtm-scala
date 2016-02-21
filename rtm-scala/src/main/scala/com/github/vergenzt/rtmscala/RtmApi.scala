package com.github.vergenzt.rtmscala

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import scalaj.http.Http
import scalaj.http.HttpRequest

import util._
import util.ParamConversions._
import util.XmlConversions._
import meta._

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
@GenerateRtmApi
object RtmApi extends RtmApiBase {
}
