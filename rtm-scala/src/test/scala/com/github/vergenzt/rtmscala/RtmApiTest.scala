package com.github.vergenzt.rtmscala

import java.util.logging.Level
import java.util.logging.Logger
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.squareup.okhttp.mockwebserver.RecordedRequest
import scalaj.http.HttpConstants

class RtmApiTest extends FunSpec with BeforeAndAfter with MockitoSugar with ScalaFutures {

  var server: MockWebServer = _

  // Turn off webserver logging. A reference has to be kept around so it doesn't
  // get garbage-collected, causing a new logger to be created. (I think. I had
  // issues with the logger not actually turning off when I didn't keep the
  // reference.
  val _logger = Logger.getLogger(classOf[MockWebServer].getName)
  _logger.setLevel(Level.OFF)

  before {
    server = new MockWebServer()
    server.start()
    RtmApi.REST_URL = server.getUrl("/rest/").toExternalForm()
  }
  after {
    server.shutdown()
  }

  def enqueueResponse(response: String) = {
    server.enqueue(new MockResponse().setBody(response))
  }

  def checkParamsIncluded(request: RecordedRequest, _params: Map[String,String]) = {
    val params = request.getPath.split('?').last.split('&')
      .map(_.split('=').map(HttpConstants.urlDecode(_, HttpConstants.utf8)))
      .map(kv => (kv(0) -> kv(1))).toMap
    _params foreach { case (k, v) =>
      assert ((k, params.get(k)) == (k, Some(v)))
    }
  }

  describe("rtm") {

    implicit val apiCreds = ApiCreds("abc123", "BANANAS")
    implicit val authToken = AuthToken("314159", Permission.Delete, User("1", "bob", Some("Bob T. Monkey")))
    implicit val timeline = Timeline("54321")

    describe("auth") {
      it("checkToken") {
        import util._
        import XmlConversions._

        enqueueResponse(s"""
          <rsp stat="ok"><auth>
            <token>${authToken.token}</token>
            <perms>delete</perms>
            <user id="1" username="bob" fullname="Bob T. Monkey" />
          </auth></rsp>
        """.trim)

        assert (rtm.auth.checkToken(authToken.token).as[AuthToken] == authToken)

        checkParamsIncluded(server.takeRequest, Map(
          "api_key" -> apiCreds.apiKey,
          "auth_token" -> authToken.token,
          "method" -> "rtm.auth.checkToken"
        ))
      }
    }
  }
}
