# rtm-scala [![Build Status](https://travis-ci.org/vergenzt/rtm-scala.svg?branch=master)](https://travis-ci.org/vergenzt/rtm-scala)

This is a Scala wrapper for the [Remember the Milk API](https://www.rememberthemilk.com/services/api/).

## Installation

```
libraryDependencies += "com.github.vergenzt" %% "rtm-scala" % "0.1.3"
```
Or in Gradle:
```
compile "com.github.vergenzt:rtm-scala_2.11:0.1.3"
```

The project is currently only built with Scala 2.11. If you'd like a build for a prior version, let me know and I'll set it up.
 
## Usage

The actual API implementation consists entirely of Scala `objects` rather than `classes`. Implicit parameters are used to pass API keys and authentication tokens.

Example (desktop authentication):
```scala
import com.github.vergenzt.rtmscala._

implicit val apiCreds = ApiCreds("<api_key>", "<secret>")
implicit val authToken = {
  val frob = rtm.auth.getFrob()
  println("Go to this url and authenticate:")
  println(rtm.auth.getURL(Permission.Read, frob))
  StdIn.readLine("Press Enter when done.")
  rtm.auth.getToken(frob)
}

val tasks = rtm.tasks.getList("status:incomplete")
tasks.foreach(task => println(task.name))
```

Alternatively, you can authenticate by passing in a function taking the string URL, returning a future to direct the user to the URL (which completes when the user has authenticated) using `rtm.auth.authenticate(Permission)(String => Future[Unit]): Future[AuthToken]`.

## Timelines

Methods that require a [`Timeline`](https://www.rememberthemilk.com/services/api/timelines.rtm) take it implicitly. So once you have `ApiCreds` and an `AuthToken` in implicit scope just declare one as follows:
```scala
implicit val timeline = rtm.timelines.create()
```

Timelines are the only mutable objects in the whole library, which maintain a sequence of `Transactions`, one for each method in which they are involved. `Transactions` that are `undoable` can be passed to `rtm.transactions.undo` to undo them.

Example:
```scala
// ... authenticate, etc.
implicit val timeline = rtm.timelines.create()

// do something you regret...
val task = rtm.tasks.getList().head
rtm.tasks.delete(task)

// undo it!
val transaction = timeline.transactions.last
rtm.transactions.undo(transaction)
```

## Progress

The following methods are not yet implemented:

 * `rtm.settings.*`
 * `rtm.time.*`
 * `rtm.timezones.*`
 * `rtm.reflection.*`

All other API methods are implemented, though not all are thoroughly tested.

## Building / Contributing

Gradle does not have to be installed on your system for any of this.

To build and run tests on the console:
```
./gradlew build
```

To create Eclipse project configuration:
```
./gradlew eclipse
```

Feel free to submit issues or pull requests and I'll respond as soon as I can. :)
