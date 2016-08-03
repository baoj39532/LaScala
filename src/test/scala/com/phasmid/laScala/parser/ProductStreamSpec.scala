package com.phasmid.laScala.parser

import java.io.{File, FileInputStream}
import java.net.URL

import com.phasmid.laScala.Lift
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.Map
import scala.util._

/**
  * @author scalaprof
  */
class ProductStreamSpec extends FlatSpec with Matchers {
  """"Hello", "World!"""" should "be (String) stream via CSV" in {
    val c = CSV[Tuple1[String]](Stream("x",""""Hello"""", """"World!""""), None)
    c.header shouldBe List("x")
    val wts = c.tuples
    wts.head match {
      case Tuple1(s) => assert(s == "Hello")
    }
    wts.tail.head match {
      case Tuple1(s) => assert(s == "World!")
    }
  }
  it should "be (String) stream via TupleStream" in {
    val wts = TupleStream[Tuple1[String]](Stream("x",""""Hello"""", """"World!""""), None).tuples
    wts.head match {
      case Tuple1(s) => assert(s == "Hello")
    }
    wts.tail.head match {
      case Tuple1(s) => assert(s == "World!")
    }
  }
  it should "convert to list properly" in {
    val c = CSV[Tuple1[String]](Stream("x",""""Hello"""", """"World!""""), None)
    val wts = c.asList
    wts.size should be(2)
  }
  it should "convert to map properly" in {
    val c = CSV[Tuple1[String]](Stream("x",""""Hello"""", """"World!""""), None)
    val wtIm = c toMap { case Tuple1(s) => s.hashCode }
    wtIm.get("Hello".hashCode) should matchPattern { case Some(Tuple1("Hello")) => }
  }
  it should "have column x of type String" in {
    val c = CSV[Tuple1[String]](Stream("x",""""Hello"""", """"World!""""), None)
    c column[String] "x" match {
      case Some(xs) =>
        xs.take(2).toList.size should be(2)
        xs.head shouldBe "Hello"
        xs(1) shouldBe "World!"
      case _ => fail("no column projected")
    }
  }
  """"3,5", "8,13"""" should "be (Int,Int) stream" in {
    val iIts = CSV[(Int, Int)](Stream("x,y", "3,5", "8,13"), None).tuples
    iIts.head match {
      case (x, y) => assert(x == 3 && y == 5)
    }
    iIts.tail.head match {
      case (x, y) => assert(x == 8 && y == 13)
    }
  }
  it should "be (String,String) stream via TupleStream" in {
    val wWts = TupleStream[(String, String)](Stream("x,y", "3,5", "8,13"), None).tuples
    wWts.head match {
      case (x, y) => assert(x == "3" && y == "5")
    }
    wWts.tail.head match {
      case (x, y) => assert(x == "8" && y == "13")
    }
  }
  it should "map into (Int,Int) via TupleStream" in {
    val wWts = TupleStream[(String, String)](Stream("x,y", "3,5", "8,13"), None)
    val iIts = wWts map { case (x, y) => (x.toInt, y.toInt) }
    iIts.tuples.head match {
      case (x, y) => assert(x == 3 && y == 5)
      case _ => fail("no match")
    }
  }
  it should "have column y of type Int" in {
    CSV[(Int, Int)](Stream("x,y", "3,5", "8,13"), None) column[Int] "y" match {
      case Some(ys) =>
        ys.take(2).toList.size should be(2)
        ys.head shouldBe 5
        ys(1) shouldBe 13
      case _ => fail("no column projected")
    }
  }
  it should "convert to map properly" in {
    val c = CSV[(Int, Int)](Stream("x,y", "3,5", "8,13"), None)
    val iItIm = c toMap { case (x, y) => x }
    iItIm.get(8) should matchPattern { case Some((8, 13)) => }
  }
  it should "map into (Double,Double) properly" in {
    val c = CSV[(Int, Int)](Stream("x,y", "3,5", "8,13"), None)
    val doubles = c map { case (x, y) => (x.toDouble, y.toDouble) }
    val dDts = doubles.tuples
    dDts.head match {
      case (x, y) => assert(x == 3.0 && y == 5.0)
    }
  }
  it should "convert into maps properly" in {
    val zWms = CSV[(Int, Int)](Stream("x,y", "3,5", "8,13"), None).asMaps
    zWms.head should be(Map("x" -> 3, "y" -> 5))
    zWms(1) should be(Map("x" -> 8, "y" -> 13))
  }
  """"3,5.0", "8,13.5"""" should "be (Int,Double) stream" in {
    val dIts = CSV[(Int, Double)](Stream("x,y", "3,5.0", "8,13.5"), None).tuples
    dIts.head match {
      case (x, y) => assert(x == 3 && y == 5.0)
    }
    dIts.tail.head match {
      case (x, y) => assert(x == 8 && y == 13.5)
    }
  }
  """dateParser""" should "work" in {
    val dp = CsvParser.dateParser
    dp("2016-03-15") should matchPattern { case Success(_) => }
  }
  """"milestone 1, 2016-03-08", "milestone 2, 2016-03-15"""" should "be (String,Datetime) stream" in {
    val dIts = CSV[(String, DateTime)](Stream("event,date", "milestone 1,2016-03-08", "milestone 2,2016-03-15"), None).tuples
    dIts.head match {
      case (x, y) => assert(x == "milestone 1" && y == new DateTime("2016-03-08"))
    }
    dIts.tail.head match {
      case (x, y) => assert(x == "milestone 2" && y == new DateTime("2016-03-15"))
    }
  }
  "sample.csv" should "be (String,Int) stream using URI" in {
    val x = getClass.getResource("sample.csv")
    println(x)
    val iWts = CSV[(String, Int)](getClass.getResource("sample.csv").toURI, None).tuples
    iWts.head match {
      case (q, y) => assert(q == "Sunday" && y == 1)
    }
    iWts.tail.head match {
      case (q, y) => assert(q == "Monday" && y == 2)
    }
    iWts.size should be(8)
    (iWts take 8).toList(7) should be("TGIF, Bruh", 8)
  }
  ignore should "be (String,Int) stream" in {
    val iWts = CSV[(String, Int)](new FileInputStream(new File("src/test/scala/com/phasmid/laScala/parser/sample.csv")), None).tuples
    iWts.head match {
      case (x, y) => assert(x == "Sunday" && y == 1)
    }
    iWts.tail.head match {
      case (x, y) => assert(x == "Monday" && y == 2)
    }
    iWts.size should be(8)
    (iWts take 8).toList(7) should be("TGIF, Bruh", 8)
  }
  ignore should "be (String,Int) stream using File" in {
    val iWts = CSV[(String, Int)](new File("sample.csv"), None).tuples
    iWts.head match {
      case (x, y) => assert(x == "Sunday" && y == 1)
    }
    iWts.tail.head match {
      case (x, y) => assert(x == "Monday" && y == 2)
    }
    iWts.size should be(8)
    (iWts take 8).toList(7) should be("TGIF, Bruh", 8)
  }
}

class CsvParserSpec extends FlatSpec with Matchers with Inside {
  val defaultParser = CsvParser()
  "CsvParser()" should """parse "x" as Success(List("x"))""" in {
    defaultParser.parseRow(""""x"""") should matchPattern { case scala.util.Success(List("x")) => }
  }
  it should """parse "x,y" as Success(List("x,y"))""" in {
    defaultParser.parseRow(""""x,y"""") should matchPattern { case scala.util.Success(List("x,y")) => }
  }
  it should """parse "x,y" as Success(List("x","y")""" in {
    defaultParser.parseRow("x,y") should matchPattern { case scala.util.Success(List("x", "y")) => }
  }
  val pipeParser = CsvParser("|")
  """"CsvParser("|")"""" should """parse "|" as Success(List("|"))""" in {
    pipeParser.parseRow(""""|"""") should matchPattern { case scala.util.Success(List("|")) => }
  }
  it should """parse x,y as Success(List("x,y"))""" in {
    pipeParser.parseRow("x,y") should matchPattern { case scala.util.Success(List("x,y")) => }
  }
  it should """parse x,y as Success(List("x","y")""" in {
    pipeParser.parseRow("x|y") should matchPattern { case scala.util.Success(List("x", "y")) => }
  }
  val customParser = CsvParser("|", "'")
  """"CsvParser("|","'")"""" should """parse '|' as Success(List("|"))""" in {
    customParser.parseRow("'|'") should matchPattern { case scala.util.Success(List("|")) => }
  }
  it should """parse x,y as Success(List("x,y"))""" in {
    customParser.parseRow("x,y") should matchPattern { case scala.util.Success(List("x,y")) => }
  }
  it should """parse x,y as Success(List("x","y")""" in {
    customParser.parseRow("x|y") should matchPattern { case scala.util.Success(List("x", "y")) => }
  }
  "CsvParser.parseElem" should "parse 1 as 1" in (CsvParser.defaultParser("1") should matchPattern { case Success(1) => })
  it should "parse 1.0 as 1.0" in (CsvParser.defaultParser("1.0") should matchPattern { case Success(1.0) => })
  it should "parse true as true" in (CsvParser.defaultParser("true") should matchPattern { case Success(true) => })
  it should "parse false as false" in (CsvParser.defaultParser("false") should matchPattern { case Success(false) => })
  it should "parse yes as yes" in (CsvParser.defaultParser("yes") should matchPattern { case Success(true) => })
  it should "parse no as false" in (CsvParser.defaultParser("no") should matchPattern { case Success(false) => })
  it should "parse T as true" in (CsvParser.defaultParser("T") should matchPattern { case Success(true) => })
  it should """parse "1" as "1"""" in (CsvParser.defaultParser(""""1"""") should matchPattern { case Success("1") => })
  it should """parse 2016-03-08 as datetime""" in {
    val dt = CsvParser.defaultParser("2016-03-08")
    dt should matchPattern { case Success(d) => }
    //    dt.get shouldBe new DateTime("2016-03-08")
  }

  def putInQuotes(w: String): Any = s"""'$w'"""

  val customElemParser = CsvParser(parseElem = Lift(putInQuotes _))
  "custom element parser" should "parse 1 as '1'" in (customElemParser.elementParser("1") should matchPattern { case Success("'1'") => })
  it should "parse 1.0 as '1.0'" in (customElemParser.elementParser("1.0") should matchPattern { case Success("'1.0'") => })
  it should "parse true as 'true'" in (customElemParser.elementParser("true") should matchPattern { case Success("'true'") => })
  it should """parse "1" as '"1"'""" in (customElemParser.elementParser(""""1"""") should matchPattern { case Success("""'"1"'""") => })

  "CsvParser.parseDate" should "work" in {
    val dt = CsvParser.parseDate(CsvParser.dateFormatStrings)("2016-03-08")
    dt should matchPattern { case Success(x) => }
    dt.get shouldBe new DateTime("2016-03-08T00:00:00.0")
  }

  "content of quotes.csv" should "work" in {
    val row = """"Apple Inc.",104.48,"8/2/2016",12.18"""
    val xs: Try[(String, Double, DateTime, Double)] = for {
      ws <- defaultParser.parseRow(row)
      x <- TupleStream.seqToTuple[(String, Double, DateTime, Double)](ws)(CsvParser.defaultParser)
    } yield x
    xs should matchPattern { case Success(("Apple Inc.",104.48,_,12.18)) => }
  }
  "quotes.csv" should "work from local URL" in {
    val url = getClass.getResource("quotes.csv")
    val csv = CSV.apply[(String, Double, DateTime, Double)](defaultParser, url.toURI, Some(Seq("name", "lastTradePrice", "lastTradeDate", "P/E ratio")))
    val x = csv.tuples
    x.size shouldBe 1
    x.head should matchPattern { case ("Apple Inc.", 104.48, _, 12.18) => }
  }
  it should "work from URL stream" in {
    val url = new URL("http://download.finance.yahoo.com/d/quotes.csv?s=AAPL&f=nl1d1r&e=.csv")
    val csv = CSV.apply[(String, Double, DateTime, Double)](defaultParser, url, Some(Seq("name", "lastTradePrice", "lastTradeDate", "P/E ratio")))
    val x = csv.tuples
    x.size shouldBe 1
    x.head should matchPattern { case ("Apple Inc.", 104.48, _, 12.18) => }
  }
  it should "yield maps from local URL" in {
    val url = getClass.getResource("quotes.csv")
    val csv = CSV.apply[(String, Double, DateTime, Double)](defaultParser, url.toURI, Some(Seq("name", "lastTradePrice", "lastTradeDate", "P/E ratio")))
    val x = csv.asMaps
    x.size shouldBe 1
    inside(x.head) {
      case m =>
        m.size shouldBe 4
        m("name") shouldBe "Apple Inc."
    }
  }
}
