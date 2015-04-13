package rtmscala.util

import rtmscala._

/**
 * Implicit parameters => option conversions.
 *
 * This serves to make the implicit parameters to `rtm.request` that are of
 * type Option[T] actually optional. If there is an implicit parameter of type T
 * in scope, then it is converted to an Option[T] below, and passed to `request`.
 * If not, then the noT value of None below will be found implicitly and passed.
 */
object OptionConversions {

  implicit def token2OptionToken(token: AuthToken): Option[AuthToken] = Some(token)
  implicit val noToken: Option[AuthToken] = None

  implicit def timeline2OptionTimeline(timeline: Timeline): Option[Timeline] = Some(timeline)
  implicit val noTimeline: Option[Timeline] = None

}
