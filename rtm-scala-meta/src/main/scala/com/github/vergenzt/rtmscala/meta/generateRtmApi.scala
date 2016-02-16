package com.github.vergenzt.rtmscala.meta

import scala.annotation.StaticAnnotation

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

class generateRtmApi extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GenerateRtmApiImpl.generateRtmApiImpl
}

object GenerateRtmApiImpl {
  def generateRtmApiImpl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    annottees.map(_.tree) match {
      case List(q"trait RtmApiGenerated") =>

        c.Expr[Any](q"""
          trait RtmApiGenerated {
            def testMethod() = println("Yo yo yo")
          }
        """)
    }
  }
}
