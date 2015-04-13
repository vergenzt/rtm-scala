package rtmscala.util

import rtmscala._

/**
 * Domain object => parameter conversions.
 *
 * The purpose of these is to make it possible to pass domain objects
 * directly into `rtm.request`, writing `rtm.request(...)(list)` rather
 * than `rtm.request(...)("list_id" -> list.id)`.
 *
 * For options, there is the implicit conversion to RtmOptionOps, which has a method
 * `toParamSeq` allowing you to write `rtm.request(...)(..., opt.toParamSeq: _*)`
 */
object ParamConversions {

  /**
   * Convert an Option[T] to a Seq[(String,String)].
   */
  implicit class RtmOptionOps[T](opt: Option[T]) {
    def toParamSeq(implicit t2Param: T => (String,String)) = opt.map(t2Param).toSeq
  }

  /**
   * Task is the only type that passes multiple parameters, so if you have a Task, pass it
   * like you do Options: `rtm.request(...)(task: _*, ...)`
   */
  implicit def task2ParamSeq(task: Task) = Seq(
    "list_id" -> task.listId,
    "taskseries_id" -> task.seriesId,
    "task_id" -> task.id
  )

  implicit def authToken2Param(authToken: AuthToken) = ("auth_token" -> authToken.token)
  implicit def frob2Param(frob: Frob)                = ("frob" -> frob.frob)
  implicit def list2Param(list: List)                = ("list_id" -> list.id)
  implicit def perms2Param(perms: Permission)        = ("perms" -> perms.name)
  implicit def timeline2Param(timeline: Timeline)    = ("timeline" -> timeline.id)
}
