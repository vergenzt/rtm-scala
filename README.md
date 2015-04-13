# rtm-scala [![Build Status](https://travis-ci.org/vergenzt/rtm-scala.svg?branch=master)](https://travis-ci.org/vergenzt/rtm-scala)

This is a work-in-progress Scala wrapper for the [Remember the Milk API](https://www.rememberthemilk.com/services/api/).

Currently implemented:
 * authentication (including an authentication flow method)
 * [timeline](https://www.rememberthemilk.com/services/api/timelines.rtm) creation
 * task creation, completion, and deletion
 * list creation
 
## Usage

The actual API implementation consists entirely of Scala `objects` rather than `classes`. Implicit parameters are used to pass API keys and authentication tokens.

Example (desktop authentication):
```scala
implicit val apiCreds = ApiCreds(<api_key>, <secret>)

val authTokenFuture = rtm.auth.authenticate(
  // The desired permission. ("read" < "write" < "delete")
  Permission("read"),
  // Function (type String => Future[Unit]) to direct the user to the url. The
  // future should complete once they're done authenticating at the website.
  (url) => Future[Unit] {
    println("Authenticate at the following url. Press Enter when finished.")
    println(url)
    StdIn.readLine
  }
)

authTokenFuture onSuccess { implicit authToken =>
  // Do whatever you want here. ApiCreds and AuthToken are now in implicit scope.
  val tasks: Seq[Task] = rtm.tasks.getList  // [will be implemented soon]
  
  implicit val timeline: Timeline = rtm.timelines.create
  rtm.tasks.complete(tasks.head)
}
```

## Building

```
./gradlew build   # for console builing and testing
./gradlew eclipse # for eclipse project file generation
```

Gradle does not have to be installed on your system.
