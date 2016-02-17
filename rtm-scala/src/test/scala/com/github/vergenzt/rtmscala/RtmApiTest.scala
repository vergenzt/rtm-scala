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
    rtm.REST_URL = server.getUrl("/rest/").toExternalForm()
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

    describe("test_dynamic") {
      it("works") {
        rtm.test
      }
    }

    describe("auth") {

      it("signs correctly") {
        import util.RtmHttpRequestOps

        val request = rtm.request("sigtest", "param1" -> "xyz", "param2" -> "123")

        val params = request.params.toMap
        val _params = Map(
          "api_key" -> apiCreds.apiKey,
          "method" -> "rtm.sigtest",
          "param1" -> "xyz",
          "param2" -> "123",
          "api_sig" -> "02c04e204a9d7f4aad86468e74412f50"
        )
        _params.foreach { case (k,v) =>
          assert (params.get(k) == Some(v))
        }
      }

      it("getFrob") {
        enqueueResponse("""<rsp stat="ok"><frob>123456</frob></rsp>""")

        val frob = rtm.auth.getFrob
        assert (frob == Frob("123456"))

        checkParamsIncluded(server.takeRequest, Map(
          "api_key" -> apiCreds.apiKey,
          "method" -> "rtm.auth.getFrob",
          "api_sig" -> "2eb41243b94f6be134b1120623ca6876"
        ))
      }

      it("getToken") {
        enqueueResponse("""
          <rsp stat="ok"><auth>
            <token>314159</token>
            <perms>delete</perms>
            <user id="1" username="bob" fullname="Bob T. Monkey" />
          </auth></rsp>
        """.trim)

        assert (
          rtm.auth.getToken(Frob("123456"))
          ==
          AuthToken("314159", Permission.Delete, User("1", "bob", Some("Bob T. Monkey")))
        )

        checkParamsIncluded(server.takeRequest, Map(
          "api_key" -> apiCreds.apiKey,
          "method" -> "rtm.auth.getToken",
          "frob" -> "123456",
          "api_sig" -> "6dd557b1cbb1725334fa513760a75cdd"
        ))
      }

      it("checkToken") {
        enqueueResponse(s"""
          <rsp stat="ok"><auth>
            <token>${authToken.token}</token>
            <perms>delete</perms>
            <user id="1" username="bob" fullname="Bob T. Monkey" />
          </auth></rsp>
        """.trim)

        assert (rtm.auth.checkToken(authToken) == authToken)

        checkParamsIncluded(server.takeRequest, Map(
          "api_key" -> apiCreds.apiKey,
          "auth_token" -> authToken.token,
          "method" -> "rtm.auth.checkToken"
        ))
      }
    }

    describe("timelines") {
      it("create") {
        enqueueResponse(s"""
          <rsp stat="ok"><timeline>${timeline.id}</timeline></rsp>
        """.trim)

        assert (rtm.timelines.create == timeline)

        checkParamsIncluded(server.takeRequest, Map(
          "api_key" -> apiCreds.apiKey,
          "method" -> "rtm.timelines.create",
          "auth_token" -> authToken.token
        ))
      }
    }
  }
}
