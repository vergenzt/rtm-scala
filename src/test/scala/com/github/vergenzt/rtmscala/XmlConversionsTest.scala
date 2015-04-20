package com.github.vergenzt.rtmscala

import scala.xml.Utility
import scala.xml.XML
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import util.XmlConversions._
import scala.xml.NodeSeq
import org.joda.time.DateTime

class XmlConversionsTest extends FlatSpec with Matchers {

  implicit def str2Xml(str: String) = Utility.trim(XML.loadString(str))

  /* Authentication */

  "xml2AuthToken" should "work" in {
    xml2AuthToken("""
      <auth>
        <token>6410bde19b6dfb474fec71f186bc715831ea6842</token>
        <perms>delete</perms>
        <user id="987654321" username="bob" fullname="Bob T. Monkey" />
      </auth>
    """) should be (
      AuthToken(
        "6410bde19b6dfb474fec71f186bc715831ea6842",
        Permission.Delete,
        User("987654321", "bob", Some("Bob T. Monkey"))
      )
    )
  }

  "xml2Frob" should "work" in {
    xml2Frob("""<frob>123456</frob>""") should be (Frob("123456"))
  }

  "xml2Perms" should "work" in {
    xml2Perms("""<perms>read</perms>""") should be (Permission.Read)
    xml2Perms("""<perms>write</perms>""") should be (Permission.Write)
    xml2Perms("""<perms>delete</perms>""") should be (Permission.Delete)
  }

  "xml2User" should "work" in {
    xml2User("""<user id="987654321" username="bob" fullname="Bob T. Monkey" />""") should be
      (User("987654321", "bob", Some("Bob T. Monkey")))
    xml2User("""<user id="987654321"><username>bob</username></user>""") should be
      (User("987654321", "bob", None))
  }

  /* Main classes */

  "xml2Contact" should "work" in {
    xml2Contact("""<contact id="1" fullname="Omar Kilani" username="omar"/>""") should be
      (Contact("1", "omar", "Omar Kilani"))
  }

  "xml2ContactSeq" should "work" in {
    xml2ContactSeq("""<contacts>
      <contact id="1" fullname="Omar Kilani" username="omar"/>
    </contacts>""") should be (Seq(
      Contact("1", "omar", "Omar Kilani")
    ))
  }

  "xml2Group" should "work" in {
    xml2Group("""<group id="987654321" name="Friends">
      <contacts />
    </group>""") should be (
      Group("987654321", "Friends", Seq())
    )
    xml2Group("""<group id="987654321" name="Friends">
      <contacts>
        <contact id="1"/>
      </contacts>
    </group>""") should be (
      Group("987654321", "Friends", Seq("1"))
    )
  }

  "xml2GroupSeq" should "work" in {
    xml2GroupSeq("""<groups>
      <group id="987654321" name="Friends">
        <contacts>
          <contact id="1"/>
        </contacts>
      </group>
    </groups>""") should be (
      Seq(Group("987654321", "Friends", Seq("1")))
    )
  }

  "xml2List" should "work" in {
    xml2List("""
      <list id="987654321" name="New List"
       deleted="0" locked="0" archived="0" position="0" smart="0"/>
    """) should be (
      List("987654321", "New List", deleted=false, locked=false,
          archived=false, position=0, filter=None)
    )
    xml2List("""
      <list id="387546" name="New List"
       deleted="0" locked="0" archived="0" position="0" smart="0" />
    """) should be (
      List("387546", "New List", deleted=false, locked=false,
          archived=false, position=0, filter=None)
    )
    xml2List("""
      <list id="100657" name="Sent"
       deleted="0" locked="1" archived="0" position="1" smart="0" />
    """) should be (
      List("100657", "Sent", deleted=false, locked=true,
          archived=false, position=1, filter=None)
    )
    xml2List("""
      <list id="387549" name="High Priority"
           deleted="0" locked="0" archived="0" position="0" smart="1">
        <filter>(priority:1)</filter>
      </list>
    """) should be (
      List("387549", "High Priority", deleted=false, locked=false,
          archived=false, position=0, filter=Some("(priority:1)"))
    )
  }

  "xml2ListSeq" should "work" in {
    xml2ListSeq("""<lists>
      <list id="387549" name="High Priority"
           deleted="0" locked="0" archived="0" position="0" smart="1">
        <filter>(priority:1)</filter>
      </list>
    </lists>""") should be (
      Seq(List("387549", "High Priority", deleted=false, locked=false,
          archived=false, position=0, filter=Some("(priority:1)")))
    )
  }

  "xml2LocationSeq" should "work" in {
    xml2LocationSeq("""
      <locations>
        <location id="987654321" name="Berlin" longitude="13.411508"
                latitude="52.524008" zoom="9" address="Berlin, Germany" viewable="1"/>
        <location id="987654322" name="New York" longitude="-74.00713"
                latitude="40.71449" zoom="9" address="New York, NY, USA" viewable="1"/>
        <location id="987654323" name="Sydney" longitude="151.216667"
                 latitude="-33.8833333" zoom="7"
                 address="Sydney, New South Wales, Australia" viewable="1"/>
      </locations>
    """) should be (Seq(
      Location("987654321", "Berlin", 52.524008, 13.411508, 9,
          "Berlin, Germany", true),
      Location("987654322", "New York", 40.71449, -74.00713, 9,
          "New York, NY, USA", true),
      Location("987654323", "Sydney", -33.8833333, 151.216667, 7,
          "Sydney, New South Wales, Australia", true)
    ))
  }

  "xml2Note" should "work" in {
    xml2Note("""
      <note id="169624"
      created="2006-05-07T11:26:49Z" modified="2006-05-07T11:26:49Z"
      title="Note Title">Note Body</note>
    """) should be (
      Note("169624", "Note Title", "Note Body",
          util.parseDateTime("2006-05-07T11:26:49Z"),
          util.parseDateTime("2006-05-07T11:26:49Z"))
    )
    xml2Note("""
      <note id="169624"
        created="2006-05-07T11:26:49Z" modified="2006-05-07T11:28:52Z"
        title="New Note Title">New Note Body</note>
    """) should be (
      Note("169624", "New Note Title", "New Note Body",
          util.parseDateTime("2006-05-07T11:26:49Z"),
          util.parseDateTime("2006-05-07T11:28:52Z"))
    )
  }

  "xml2TaskSeq" should "work" in {
    xml2TaskSeq("""
      <tasks>
        <list id="987654321" current="2006-05-07T08:13:26Z">
          <taskseries id="123456789" created="2006-05-07T10:19:54Z" modified="2006-05-07T10:19:54Z"
                     name="Get Bananas" source="api" url="" location_id="">
            <tags/>
            <participants/>
            <notes/>
            <task id="987654321" due="" has_due_time="0" added="2006-05-07T10:19:54Z"
                 completed="" deleted="" priority="N" postponed="0" estimate=""/>
          </taskseries>
          <deleted>
            <taskseries id="650390">
              <task id="815255" deleted="2006-05-07T14:26:47Z" />
            </taskseries>
          </deleted>
        </list>
      </tasks>
    """) should be (Seq(
      Task(
        "987654321", "123456789", "987654321",
        name = "Get Bananas",
        due = None, hasDueTime = false,
        tags = Seq(),
        url = None,
        priority = 0,
        postponed = 0,
        estimate = None,
        source = "api",
        notes = Seq(),
        participants = Seq(),
        created = util.parseDateTime("2006-05-07T10:19:54Z"),
        modified = util.parseDateTime("2006-05-07T10:19:54Z"),
        added = util.parseDateTime("2006-05-07T10:19:54Z"),
        completed = None,
        deleted = None
      )
    ))
  }

  /* Timelines and transactions */

  "xml2Timeline" should "work" in {
    xml2Timeline("""<timeline>12741021</timeline>""") should be
      (Timeline("12741021"))
  }

}
