package rtmscala

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.io.StdIn
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.Span

class RememberTheMilkTest extends FunSpec with ScalaFutures {
  import util.OptionConversions._

  implicit val apiCreds = {
    val istream = getClass.getResourceAsStream("/api_credentials.txt")
    val lines = Source.fromInputStream(istream).getLines
      .map(_.takeWhile(_ != '#').trim).filterNot(_ == "")
    ApiCreds(lines.next(), lines.next())
  }

  def directUserToURL(url: String) = Future[Unit] {
    println("Go to the following URL, and press Enter once authenticated:")
    println("  " + url)
    StdIn.readLine
  }

  describe("Remember the Milk API") {

    it("rtm.test.echo") {
      val response = rtm.test.echo
      assert (response == s"""
        <method>rtm.test.echo</method>
        <api_key>${apiCreds.apiKey}</api_key>
      """.trim.lines.map(_.trim).mkString)
    }

    ignore("rtm.test.login") {
      implicit val authToken = rtm.auth.authenticate(Permission("read"), directUserToURL)
        .futureValue(timeout(Span.Max))

      val test = rtm.test.login
      assert (test.username == authToken.user.username)
    }
  }
}
