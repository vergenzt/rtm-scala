package com.github.vergenzt.rtmscala.util

import java.net._
import java.util._
import java.io._

object HttpCaching {
  /** Set up caching of http responses to decrease build times */
  def setUp() = {
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
