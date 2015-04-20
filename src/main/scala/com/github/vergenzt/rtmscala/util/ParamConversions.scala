package com.github.vergenzt.rtmscala.util

import com.github.vergenzt.rtmscala._

/**
 * Domain object => parameter conversions.
 *
 * The purpose of these is to make it possible to pass domain objects
 * directly into `rtm.request`, writing `rtm.request(...)(list)` rather
 * than `rtm.request(...)("list_id" -> list.id)`.
 */
object ParamConversions {
  /**
   * Task is the only type that passes multiple parameters, so if you have a Task, pass it
   * with vararg expansion: `rtm.request(..., task: _*)`
   */
  implicit def task2ParamSeq(task: Task) = Seq(
    "list_id" -> task.listId,
    "taskseries_id" -> task.seriesId,
    "task_id" -> task.id
  )

  implicit def authToken2Param(authToken: AuthToken) = ("auth_token" -> authToken.token)
  implicit def contact2Param(contact: Contact)       = ("contact_id" -> contact.id)
  implicit def frob2Param(frob: Frob)                = ("frob" -> frob.frob)
  implicit def group2Param(group: Group)             = ("group_id" -> group.id)
  implicit def list2Param(list: List)                = ("list_id" -> list.id)
  implicit def perms2Param(perms: Permission)        = ("perms" -> perms.name)
  implicit def timeline2Param(timeline: Timeline)    = ("timeline" -> timeline.id)
}
