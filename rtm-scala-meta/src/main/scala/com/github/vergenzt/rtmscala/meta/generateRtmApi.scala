package com.github.vergenzt.rtmscala.meta

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import java.net._
import com.github.vergenzt.rtmscala._
import com.github.vergenzt.rtmscala.util._
import com.github.vergenzt.rtmscala.util.XmlConversions._
import scala.annotation.compileTimeOnly

@compileTimeOnly("This is a macro requiring macro paradise.")
class GenerateRtmApi extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GenerateRtmApiImpl.generateRtmApiImpl
}

object GenerateRtmApiImpl {
  implicit val creds = ApiCreds(System.getProperty("rtm.api_key"), System.getProperty("rtm.api_secret"))

  /* Useful utility methods */
  private implicit class MethodDescExtra(method: MethodDesc) {
    def nameParts: Array[String] = method.name.split('.').drop(1)
    def group: String = nameParts.head
    def methodName: String = nameParts.tail.mkString(".")
  }

  /** Generate an implementation for RtmApi. */
  def generateRtmApiImpl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    setUpRtmCaching()

    annottees.map(_.tree) match {
      case scala.List(q"object $rtm extends $rtmBase { ..$rtmCustomImpl }") =>

        val methods = RtmApiBase.reflection.getMethods().filter(_.group != "reflection")

        // make an object for each group and an implementation for each method
        val groupImpls = methods.groupBy(_.group)
          .map({ case (group, methods) =>
            val methodImpls = methods.map(method => {
              q"""def ${TermName(method.methodName)}() = println(${method.methodName})"""
            })

            q"""
              object ${TermName(group)} {
                ..$methodImpls
              }
            """
          })

        // TODO merge in the custom methods

        c.Expr[Any](q"""
          object $rtm extends $rtmBase {
            ..$groupImpls
          }
        """)
    }
  }

  /** Set up caching of RTM reflection api responses to decrease build times */
  def setUpRtmCaching() = {
    import java.net._
    import java.util._
    import java.io._
    ResponseCache.setDefault(new ResponseCache() {
      val cacheDir = System.getProperty("user.home") + "/.rtm-scala-meta/build-cache"
      new File(cacheDir).mkdirs()
      def get(uri: URI, method: String, headers: Map[String, List[String]]) =
        try {
          val reader = new FileInputStream(cacheDir + "/" + uri.getQuery())
          val headers = new ObjectInputStream(reader).readObject().asInstanceOf[Map[String,List[String]]]
          new CacheResponse {
            def getBody(): InputStream = reader
            def getHeaders(): Map[String,List[String]] = headers
          }
        } catch {
          case _: IOException => null
        }
      def put(uri: URI, conn: URLConnection) =
        try {
          val writer = new FileOutputStream(cacheDir + "/" + uri.getQuery(), true)
          new ObjectOutputStream(writer).writeObject(conn.getHeaderFields)
          new CacheRequest {
            def abort(): Unit = writer.close()
            def getBody(): OutputStream = writer
          }
        } catch {
          case _: IOException => null
        }
    })
  }
}
