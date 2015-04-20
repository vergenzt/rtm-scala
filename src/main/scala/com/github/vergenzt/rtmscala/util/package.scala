package com.github.vergenzt.rtmscala

import java.security.MessageDigest
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Utility
import scala.xml.XML
import org.joda.time.format.DateTimeFormat
import scalaj.http.HttpRequest

package object util {

  import XmlConversions._

  /** Convert a boolean to a string for use in passing as a parameter. */
  implicit def string2Bool(string: String) = string match { case "1" => true; case "0" => false }
  implicit def bool2String(bool: Boolean) = if (bool) "1" else "0"

  val rtmDateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  /**
   * Add useful methods to the HttpRequest object.
   */
  implicit class RtmHttpRequestOps(request: HttpRequest) {
    /**
     * Sign the request per RTM's specs.
     */
    def signed(implicit creds: ApiCreds): HttpRequest = {
      val text = creds.secret + request.params.sortBy(_._1).map(t => t._1 + t._2).mkString
      val digest = MessageDigest.getInstance("MD5").digest(text.getBytes())
      val sig = digest.map("%02x".format(_)).mkString
      request.param("api_sig", sig)
    }

    /**
     * Get response as domain object. If a timeline is available and has been
     * passed in the request, this will parse the transaction and add it to
     * the timeline.
     */
    def as[T](implicit xml2T: XmlParser[T], timelineNullable: Timeline = null): T = {
      val timelineOption = Option(timelineNullable)
        .filter(timeline => request.params.contains("timeline" -> timeline.id))

      val rsp = Utility.trim(request.execute(XML.load).body)
      (rsp \@ "stat") match {
        case "ok" => timelineOption match {

          // no timeline/transaction, parse response
          case None => xml2T(rsp.child)

          // there is a timeline
          case Some(timeline) => rsp.child match {

            // there is a transaction in the response
            case Seq(t @ <transaction/>, rest @ _*) =>
              timeline._transactions.append(
                Transaction(timeline, t \@ "id", t \@ "undoable", request)
              )
              xml2T(rest)

            case rest =>
              assert(false, "There was no transaction returned on a timelined method. :(")
              xml2T(rest)
          }
        }
        case "fail" =>
          val err = rsp \ "err"
          throw new RtmException(err \@ "msg", (err \@ "code").toInt)
      }
    }

    def fullURL: String = request.urlBuilder(request)
  }
}
