package com.github.vergenzt.rtmscala
package meta

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import java.net._
import scala.annotation.compileTimeOnly

import util._
import util.XmlConversions._

@compileTimeOnly("This is a macro requiring macro paradise.")
class GenerateRtmApi extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GenerateRtmApiImpl.generateRtmApiImpl
}

class GenerateRtmApiImpl(val c: Context) {
  import c.universe._
  implicit val creds = ApiCreds(System.getProperty("rtm.api_key"), System.getProperty("rtm.api_secret"))

  /* Useful utility methods */
  private implicit class MethodDescExtra(method: MethodDesc) {
    def nameParts: Array[String] = method.name.split('.').drop(1)
    def group: String = nameParts.head
    def methodName: String = nameParts.tail.mkString(".")
  }

  /** Generate an implementation for RtmApi. */
  def generateRtmApiImpl(annottees: c.Expr[Any]*): c.Expr[Any] = {
    HttpCaching.setUp()

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
}
