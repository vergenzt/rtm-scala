package rtmscala

import java.security.MessageDigest

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.XML

import org.joda.time.format.ISODateTimeFormat

import scalaj.http.HttpRequest

package object util {

  /** Convert a boolean to a string for use in passing as a parameter. */
  implicit def string2Bool(string: String) = string match { case "1" => true; case "0" => false }
  implicit def bool2String(bool: Boolean) = if (bool) "1" else "0"

  /**
   * Parse an ISO-formatted date time
   */
  def parseDateTime(string: String) = ISODateTimeFormat.dateTime().parseDateTime(string)

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

    /** Get response as domain object. */
    def as[T](implicit fromXml: NodeSeq => T) = {
      val rsp = request.execute(XML.load).body
      (rsp \@ "stat") match {
        case "ok" => fromXml(rsp.child)
        case "fail" =>
          val err = rsp \ "err"
          throw new RtmException(err \@ "msg", (err \@ "code").toInt)
      }
    }

    def fullURL: String = request.urlBuilder(request)
  }

}
