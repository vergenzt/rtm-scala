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

  def implementMethod(method: MethodDesc): c.Tree = {
    val args = method.arguments
      .filter(arg => arg.name != "api_key" && arg.name != "timeline")
      .map({
        case arg if !arg.optional => q"val ${TermName(arg.name)}: String"
        case arg if  arg.optional => q"val ${TermName(arg.name)}: Option[String] = None"
      })

    val implicitArgs = Seq(
      Some(q"implicit val creds: ApiCreds"),
      Some(q"implicit val authToken: AuthToken")
        .filter(_ => method.needsLogin),
      Some(q"implicit val timeline: Timeline")
        .filter(_ => method.arguments.exists(_.name == "timeline"))
    ).flatten

    val body = q"""
      val request = unsignedRequest(${method.fullName.stripPrefix("rtm.")},
        ..${method.arguments
          .filter(arg => arg.name != "api_key" && arg.name != "timeline")
          .map {
            // convert each arg into `"<name>" -> value
            case arg if !arg.optional => q"${arg.name} -> ${TermName(arg.name)}"
            case arg if  arg.optional => q"${arg.name} -> ${TermName(arg.name)}.get"
          }
        },
        ..${Seq(
          Some(q"""("auth_token" -> authToken.token)""")
            .filter(_ => method.needsLogin),
          Some(q"""("timeline" -> timeline.id)""")
            .filter(_ => method.arguments.exists(_.name == "timeline"))
        ).flatten}
      )

      ${if (method.needsSigning) q"request.signed" else q"request"}
    """

    q"def ${TermName(method.name)} (..$args)(..$implicitArgs) = { $body }"
  }

  /** Generate an implementation for RtmApi. */
  def generateRtmApiImpl(annottees: c.Expr[Any]*): c.Expr[Any] = {
    HttpCaching.setUp()

    annottees.map(_.tree) match {
      case scala.List(q"object $rtm extends RtmApiBase { ..$customImpls }") =>

        val methods = RtmApiBase.reflection.getMethods().filter(_.group != "reflection")

        c.Expr[Any](q"""
          object $rtm extends RtmApiBase {

            // generated method groups
            ..${ methods.groupBy(_.group).map { case (group, methods) => q"""
              object ${TermName(group)} {

                // generated methods
                ..${methods.map(implementMethod)}

                // custom methods inside generated groups
                ..${customImpls.flatMap {
                  case q"object ${TermName(group)} { ..$customMethods }" => customMethods
                  case _ => None
                }}
              }
            """}}

            // custom methods outside generated groups
            ..${customImpls.filter {
              case q"object ${TermName(group)} { ..$impl }" =>
                !methods.exists(_.group == group)
              case _ =>
                true
            }}
          }
        """)
    }
  }
}
