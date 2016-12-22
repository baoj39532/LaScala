package com.phasmid.laScala.tree

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

/**
  * TODO there's a lot of redundant tests here -- clean them out
  *
  * Created by scalaprof on 10/19/16.
  */
class KVTreeSpec extends FlatSpec with Matchers {

  implicit object IntStringKeyOps extends StringKeyOps[Int] {
    def getParentKey(t: Int): Option[String] = (for (i <- Try(t/10); s = i.toString) yield s).toOption
    def createValueFromKey(k: String): Option[Int] = Try(k.toInt).toOption
  }

  implicit object GeneralKVTreeBuilderStringInt extends GeneralKVTreeBuilder[String,Int]

  behavior of "nodeIterator"

  it should "work properly for GeneralKVTree" in {
    val tree = GeneralKVTree(Some(0), Seq(Leaf(1), Leaf(2), Leaf(3)))
    val ns = tree.nodeIterator()
    ns.size shouldBe 4
  }

  behavior of "iterator"

  it should "work properly for GeneralKVTree" in {
    val tree = GeneralKVTree(Some(0), Seq(Leaf(1), Leaf(2), Leaf(3)))
    val ns = tree.iterator(false).toList
    ns.size shouldBe 4
    ns shouldBe List(0, 1, 2, 3)
  }

  behavior of ":+(node)"

  it should "work correctly for GeneralKVTree" in {
    val tree = GeneralKVTree(Some(0), Seq(Leaf(1), Leaf(2), Leaf(3)))
    val tree2 = tree :+ Leaf(4)
    val ns = tree2.iterator(false).toList
    ns.size shouldBe 5
    ns shouldBe List(0, 1, 2, 3, 4)
  }

  behavior of ":+(value)"

  it should "work correctly for GeneralKVTree" in {
    val tree = GeneralKVTree(Some(0), Seq(Leaf(1), Leaf(2), Leaf(3)))
    val tree2 = tree :+ 4
    val ns = tree2.iterator(false).toList
    ns.size shouldBe 5
    ns shouldBe List(0, 1, 2, 3, 4)
  }

  behavior of "size"
  it should "work correctly for GeneralKVTree" in {
    val tree = GeneralKVTree(Some(0), Seq(Leaf(1), Leaf(2), Leaf(3)))
    tree.size shouldBe 4
  }

  behavior of "get"
  it should "work correctly for GeneralKVTree" in {
    val tree = GeneralKVTree(Some(0), Seq(Leaf(1), Leaf(2), Leaf(3)))
    tree.get should matchPattern { case Some(0) => }
  }

  behavior of "depth"
  it should "work correctly for GeneralKVTree" in {
    val tree = GeneralKVTree(Some(0), Seq(Leaf(1), Leaf(2), Leaf(3)))
    tree.depth shouldBe 2
  }

  behavior of "includes"
  it should "work for GeneralKVTree" in {
    val tree = GeneralKVTree[String,Int](Some(1), Seq(Leaf(2), Leaf(3)))
    tree.includesValue(1) shouldBe true
    tree.includesValue(2) shouldBe true
    tree.includesValue(3) shouldBe true
    tree.includesValue(4) shouldBe false
    tree.includesValue(0) shouldBe false
  }

  behavior of "find"
  it should "work for GeneralKVTree" in {
    val tree = GeneralKVTree[String,Int](Some(1), Seq(Leaf(2), Leaf(3)))
    tree.find(1) should matchPattern { case Some(GeneralKVTree(Some(1), Seq(Leaf(2), Leaf(3)))) => }
    tree.find(2) should matchPattern { case Some(Leaf(2)) => }
    tree.find(3) should matchPattern { case Some(Leaf(3)) => }
    tree.find(4) should matchPattern { case None => }
    tree.find(0) should matchPattern { case None => }
  }

  behavior of ":+, findParent, createValueFromKey, etc."
  // TODO this should work properly
  it should "work correctly for GeneralKVTree" in {
    val tree = GeneralKVTree(Some(0), Seq(Leaf(1), Leaf(2), Leaf(3)))
    val tree2 = tree :+ 14
    val ns = tree2.iterator(false).toList
    println(ns)
    ns.size shouldBe 5
    ns shouldBe List(0, 1, 14, 2, 3)
  }

//  val flatLandTree = "a{{about}above{{actually}ago{{alas}all{{and}another{{anything}appear{{are}as{{at}back{{be}because{{become}becoming{{been}below{{bringing}but{{by}call{{can}ceased{{circle}clearer{{condition}contrary{{correct}could{{country}countrymen{{dare}demonstrate{{described}distinguish{{down}drawing{{edge}edges{{exactly}except{{eye}far{{few}figure{{figures}find{{fixed}flatland{{flatlander}freely{{from}gradually{{happy}hard{{has}have{{hexagons}higher{{i}imagine{{impossible}in{{inhabitants}instead{{into}is{{it}its{{kind}last{{leaning}least{{like}line{{lines}live{{look}lower{{luminous}make{{middle}mind{{more}move{{moving}much{{my}nature{{necessity}nor{{not}nothing{{notion}now{{of}on{{once}one{{only}opened{{or}other{{our}oval{{over}p{{paper}penny{{pentagons}perceive{{place}placed{{places}power{{pretty}privileged{{readers}remaining{{rising}said{{say}see{{shadows}sheet{{should}sight{{sinking}so{{solid}space{{speedily}squares{{straight}such{{suppose}surface{{table}tables{{that}the{{their}them{{then}there{{things}this{{thus}to{{triangles}universe{{upon}us{{vast}very{{view}views{{visible}was{{we}were{{what}when{{which}who{{will}with{{without}world{{years}you{{your}yourself}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}"
}
