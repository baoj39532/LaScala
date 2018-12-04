/*
 * LaScala
 * Copyright (c) 2017. Phasmid Software
 */

package com.phasmid.laScala.values

import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.language.implicitConversions


/**
  * @author scalaprof
  */
class FuzzySpec extends FlatSpec with Matchers with Inside {

  behavior of "apply"
  it should "work for exact" in {
    val target = Exact(2.0)
    target() shouldBe 2
  }
  it should "work for bounded" in {
    val target = Bounded(2.0, 1.0)
    target() shouldBe 2
  }
  it should "work for Gaussian" in {
    val target = Gaussian(2.0, 0.1)
    target() shouldBe 2
  }

  behavior of "isExact"
  it should "work for exact" in {
    val target = Exact(2.0)
    target.isExact shouldBe true
  }
  it should "work for bounded" in {
    val target = Bounded(2.0, 1.0)
    target.isExact shouldBe false
  }
  it should "work for Gaussian" in {
    val target = Gaussian(2.0, 0.1)
    target.isExact shouldBe false
  }

  behavior of "p(t)"
  it should "work for exact" in {
    val target = Exact(2.0)
    target.p(2) shouldBe Probability.Certain
    target.p(2.00001) shouldBe Probability.Impossible
    target.p(4) shouldBe Probability.Impossible
  }
  it should "work for simple bounded" in {
    val target = Bounded(2.0, 1.0)
    target.p(2) shouldBe Probability(1, 2)
    target.p(4) shouldBe Probability.Impossible
  }
  // TODO test for Gaussian

  behavior of "p(t,t)"
  it should "work for exact" in {
    val target = Exact(2.0)
    target.p(1, 3) shouldBe Probability.Certain
    target.p(2.00001, 3) shouldBe Probability.Impossible
    target.p(0, 1.9999) shouldBe Probability.Impossible
  }
  it should "work for bounded" in {
    val target = Bounded(2.0, 1.0)
    target.p(0, 5) shouldBe Probability.Certain
    target.p(3, 4) shouldBe Probability.Impossible
    target.p(2, 4) shouldBe Probability(1, 2)
    target.p(2.5, 4) shouldBe Probability(1, 4)
  }
  // TODO test for Gaussian

  behavior of "compare"
  it should "work for exact" in {
    val target = Exact(2.0)
    target.compareTo(Exact(2.0)) shouldBe 0
  }
  it should "work for bounded (exact)" in {
    val target = Bounded(2.0, 1.0)
    target.compareTo(Exact(2.0)) shouldBe 0
    target.compareTo(Bounded(2.0, 1.0)) shouldBe 0
  }
  it should "work for bounded (fuzzy)" in {
    val target = Bounded(2.0, 1.0)
    target.compareTo(Bounded(1.5, 1.0)) shouldBe 0
  }
  // TODO test for Gaussian

  behavior of "map"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val target = two.map(Rational(1, 2) * _, identity)
    target.apply() shouldBe Rational.one[Int]
    target.isExact shouldBe true
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 1.0)
    val target = two.map(_ * 0.5, _ * 0.5).asInstanceOf[NumericFuzzy[Double]]
    target() shouldBe 1.0
    target.fuzziness shouldBe 0.5
    target.isExact shouldBe false
  }
  // TODO test for Gaussian

  behavior of "map2"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target = two.map2(ten)(_ * _, _ * _)
    target.apply() shouldBe Rational[Int](20)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target = two.map2(ten)(_ * _, 10.0 * _ + _)
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target = ten.map2(two)(_ * _, _ + 10.0 * _)
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target = two.map2(ten)(_ * _, _ * _)
    target() shouldBe 20.0
    target.fuzziness shouldBe 0.001
    target.isExact shouldBe false
  }
  // TODO test for Gaussian

  behavior of "plus"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two.plus(ten)
    target.apply() shouldBe Rational[Int](12)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two.plus(ten)
    target() shouldBe 12.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten.plus(two)
    target() shouldBe 12.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two.plus(ten)
    target1() shouldBe 12.0
    target1.fuzziness shouldBe 0.11
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten.plus(two)
    target2 shouldBe target1
  }
  it should "work for Gaussian" in {
    val two = Gaussian(2.0, 0.01)
    val ten = Gaussian(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two.plus(ten)
    target1() shouldBe 12.0
    // CHECK
    target1.fuzziness * target1.fuzziness shouldBe 0.1 * 0.1 + 0.01 * 0.01
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten.plus(two)
    target2 shouldBe target1
  }

  behavior of "minus"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two.minus(ten)
    target.apply() shouldBe Rational[Int](-8)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two.minus(ten)
    target() shouldBe -8.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten.minus(two)
    target() shouldBe 8.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two.minus(ten)
    target1() shouldBe -8.0
    target1.fuzziness shouldBe 0.11
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten.minus(two)
    target2() shouldBe 8.0
    target2.fuzziness shouldBe 0.11
    target2.isExact shouldBe false
  }
  // TODO test for Gaussian

  behavior of "invert"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val target = two.invert
    target shouldBe Exact(Rational(1, 2))
    target.apply() shouldBe Rational[Int](1,2)
    target.isExact shouldBe true
    target.asInstanceOf[BaseNumericFuzzy[Int]].invert shouldBe two
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 1.0E-4)
    val target: NumericFuzzy[Double] = two.invert
    target shouldBe Bounded(0.5, 0.25E-4)
    target() shouldBe 0.5
    target.fuzziness shouldBe 0.25E-4
    target.isExact shouldBe false
    target.asInstanceOf[BaseNumericFuzzy[Int]].invert shouldBe two
  }
  it should "work for exact x * x.invert equals exact(1)" in {
    val two = Exact(Rational[Int](2))
    val invertedTwo = two.invert
    val target: NumericFuzzy[Rational[Int]] = two * invertedTwo
    target shouldBe Exact(Rational[Int](1))
  }
  it should "work for bounded x * x.invert equals bounded(1, x.fuzziness)" in {
    val two = Bounded(2.0, 1.0E-4)
    val invertedTwo = two.invert
    val target: NumericFuzzy[Double] = two * invertedTwo
    target shouldBe Bounded(1.0, 1.0E-4)
  }
  // TODO test for Gaussian

  behavior of "times"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two.times(ten)
    target.apply() shouldBe Rational[Int](20)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two.times(ten)
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten.times(two)
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }
  it should "work for bounded Double" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two.times(ten)
    target1() shouldBe 20.0
    target1.fuzziness shouldBe 0.3 +- 1E-10
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten.times(two)
    target2 shouldBe target1
  }
  it should "work for bounded Rational" in {
    val two = Bounded(Rational[Int](2), Rational(1, 100))
    val ten = Bounded(Rational[Int](10), Rational(1, 10))
    val target1: NumericFuzzy[Rational[Int]] = two.times(ten)
    target1() shouldBe Rational[Int](20)
    target1.fuzziness shouldBe Rational(3, 10)
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Rational[Int]] = ten.times(two)
    target2 shouldBe target1
  }
  // TODO test for Gaussian

  behavior of "div"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two.div(ten)
    target.apply() shouldBe Rational[Int](5).invert
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two.div(ten)
    target() shouldBe 0.2
    target.fuzziness shouldBe 0.1
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten.div(two)
    target() shouldBe 5.0
    target.fuzziness shouldBe 2.5 // CHECK this
    target.isExact shouldBe false
  }
  it should "work for bounded Double" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two.div(ten)
    target1() shouldBe 0.2
    target1.fuzziness shouldBe 0.003
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten.div(two)
    target2() shouldBe 5.0
    target2.fuzziness shouldBe 0.075 +- 1E-10
    target2.isExact shouldBe false
  }
  it should "work for bounded Rational" in {
    val two = Bounded(Rational[Int](2), Rational(1, 100))
    val ten = Bounded(Rational[Int](10), Rational(1, 10))
    val target1: NumericFuzzy[Rational[Int]] = two.div(ten)
    target1() shouldBe Rational(1, 5)
    target1.fuzziness shouldBe Rational(3, 1000)
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Rational[Int]] = ten.div(two)
    target2() shouldBe Rational[Int](5)
    target2.fuzziness shouldBe Rational(3, 40)
    target2.isExact shouldBe false
  }
  it should "work for a.div(b) equals a.times(b.invert)" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two.div(ten)
    val target2: NumericFuzzy[Double] = two.times(ten.invert)
    target1 shouldBe target2
  }
  // TODO test for Gaussian

  behavior of "power(int)"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val target: Fuzzy[Rational[Int]] = two.power(5)
    target.apply() shouldBe Rational[Int](32)
    target.isExact shouldBe true
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 1.0)
    val target: NumericFuzzy[Double] = two.power(5)
    target() shouldBe 32.0
    target.fuzziness shouldBe 80.0
    target.isExact shouldBe false
  }
  it should "work for exact and negative power" in {
    val two = Exact(Rational[Int](2))
    val target = two.power(-5)
    target.apply() shouldBe Rational(1, 32)
    target.isExact shouldBe true
  }
  it should "work for bounded and negative power" in {
    val two = Bounded(2.0, 1.0)
    val target: NumericFuzzy[Double] = two.power(-5)
    target.apply() shouldBe 0.03125
    target.fuzziness shouldBe 0.078125
    target.isExact shouldBe false
  }
  it should "work for x.power(2) equals x * x" in {
    val two = Bounded(2.0, 0.01)
    val target1: NumericFuzzy[Double] = two.power(2)
    val target2: NumericFuzzy[Double] = two * two
    target1 shouldBe target2
  }
  it should "work for x.power(-1) equals x.invert" in {
    val two = Bounded(2.0, 0.01)
    val target1: NumericFuzzy[Double] = two.power(-1)
    val target2: NumericFuzzy[Double] = two.invert
    target1 shouldBe target2
  }
  //This should work
  ignore should "work for x.power(5) / x.power(2) equals x.power(3)" in {
    val two = Bounded(2.0, 0.01)
    val target1: NumericFuzzy[Double] = two.power(5).asInstanceOf[BaseNumericFuzzy[Double]] / two.power(2).asInstanceOf[BaseNumericFuzzy[Double]]
    val target2: NumericFuzzy[Double] = two.power(3)
    target1 shouldBe target2
  }
  it should "work for exact x.power(0) equals exact(ONE)" in {
    val two = Exact(Rational[Int](2))
    val target1: NumericFuzzy[Rational[Int]] = two.power(0)
    target1 shouldBe Exact(Rational.one[Int])
  }
  it should "work for bounded x.power(0) equals exact(ONE)" in {
    val two = Bounded(2.0, 0.01)
    val target1: NumericFuzzy[Double] = two.power(0)
    target1 shouldBe Exact(1.0)
  }
  // TODO test for Gaussian

  behavior of "negate"
  it should "work for exact" in {
    val target = Exact(Rational[Int](2)).negate
    target shouldBe Exact(Rational[Int](-2))
  }
  it should "work for bounded" in {
    val target = Bounded(2.0, 1.0).negate
    target shouldBe Bounded(-2.0, 1.0)
  }
  // TODO test for Gaussian

  behavior of "+"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two + ten
    target.apply() shouldBe Rational[Int](12)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two + ten
    target() shouldBe 12.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten + two
    target() shouldBe 12.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two + ten
    target1() shouldBe 12.0
    target1.fuzziness shouldBe 0.11
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten + two
    target2 shouldBe target1
  }
  // TODO test for Gaussian

  behavior of "-"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two - ten
    target.apply() shouldBe Rational[Int](-8)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two - ten
    target() shouldBe -8.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten - two
    target() shouldBe 8.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two - ten
    target1() shouldBe -8.0
    target1.fuzziness shouldBe 0.11
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten - two
    target2() shouldBe 8.0
    target2.fuzziness shouldBe 0.11
    target2.isExact shouldBe false
  }
  // TODO test for Gaussian

  behavior of "*"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two * ten
    target.apply() shouldBe Rational[Int](20)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two * ten
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten * two
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }
  it should "work for bounded Double" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two * ten
    target1() shouldBe 20.0
    target1.fuzziness shouldBe 0.3 +- 1E-10
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten * two
    target2 shouldBe target1
  }
  it should "work for bounded Rational" in {
    val two = Bounded(Rational[Int](2), Rational(1, 100))
    val ten = Bounded(Rational[Int](10), Rational(1, 10))
    val target1: NumericFuzzy[Rational[Int]] = two * ten
    target1() shouldBe Rational[Int](20)
    target1.fuzziness shouldBe Rational(3, 10)
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Rational[Int]] = ten * two
    target2 shouldBe target1
  }
  // TODO test for Gaussian

  behavior of "/"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two / ten
    target.apply() shouldBe Rational[Int](5).invert
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two / ten
    target() shouldBe 0.2
    target.fuzziness shouldBe 0.1
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten / two
    target() shouldBe 5.0
    target.fuzziness shouldBe 2.5 // CHECK this
    target.isExact shouldBe false
  }
  it should "work for bounded Double" in {
    val two = Bounded(2.0, 0.01)
    val ten = Bounded(10.0, 0.1)
    val target1: NumericFuzzy[Double] = two / ten
    target1() shouldBe 0.2
    target1.fuzziness shouldBe 0.003
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Double] = ten / two
    target2() shouldBe 5.0
    target2.fuzziness shouldBe 0.075 +- 1E-10
    target2.isExact shouldBe false
  }
  it should "work for bounded Rational" in {
    val two = Bounded(Rational[Int](2), Rational(1, 100))
    val ten = Bounded(Rational[Int](10), Rational(1, 10))
    val target1: NumericFuzzy[Rational[Int]] = two / ten
    target1() shouldBe Rational(1, 5)
    target1.fuzziness shouldBe Rational(3, 1000)
    target1.isExact shouldBe false
    val target2: NumericFuzzy[Rational[Int]] = ten / two
    target2() shouldBe Rational[Int](5)
    target2.fuzziness shouldBe Rational(3, 40)
    target2.isExact shouldBe false
  }
  // TODO test for Gaussian

  behavior of "getP"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    two.getP(Rational[Int](2)) shouldBe None
    two.getP(Rational[Int](1)) shouldBe None
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 0.5)
    two.getP(2.0) shouldBe Some(Probability.Certain)
    two.getP(0.0) shouldBe Some(Probability.Impossible)
  }
  // TODO test for Gaussian

  behavior of "Fuzzy.pow"
  it should "work for positive power" in {
    val two = Rational[Int](2)
    val target = Fuzzy.pow(two, 2)
    target shouldBe Rational[Int](4)
  }
  it should "work for negative power" in {
    val two = Rational[Int](2)
    val target = Fuzzy.pow(two, -2)
    target shouldBe Rational(1, 4)
  }
  it should "work for 0 power" in {
    val two = Rational[Int](2)
    val target = Fuzzy.pow(two, 0)
    target shouldBe Rational[Int](1)
  }
  // TODO test for Gaussian

  behavior of "power(double)"
  it should "work for exact" in {
    val two = Exact(2.0)
    val target = two.power(5.0)
    target.apply() shouldBe 32.0
    target.isExact shouldBe true
  }
  it should "work for bounded" in {
    val two = Bounded(2.0, 0.1)
    val target: NumericFuzzy[Double] = two.power(5.0)
    target() shouldBe 32.0
    target.fuzziness shouldBe 8.0
    target.isExact shouldBe false
  }
  it should "work for exact and negative power" in {
    val two = Exact(2.0)
    val target = two.power(-5)
    target.apply() shouldBe 1.0 / 32
    target.isExact shouldBe true
  }
  it should "work for bounded and negative power" in {
    val two = Bounded(2.0, 1.0)
    val target: NumericFuzzy[Double] = two.power(-5.0)
    target.apply() shouldBe 0.03125
    target.fuzziness shouldBe 0.078125
    target.isExact shouldBe false
  }
  it should "work for x.power(2.0) equals x * x" in {
    val two = Bounded(2.0, 0.01)
    val target1: NumericFuzzy[Double] = two.power(2.0)
    val target2: NumericFuzzy[Double] = two * two
    target1 shouldBe target2
  }
  it should "work for x.power(2.5).power(2.0) equals x.power(5.0)" in {
    val two = Bounded(2.0, 0.01)
    val target1: NumericFuzzy[Double] = two.power(2.5).asInstanceOf[BaseNumericFuzzy[Double]].power(2.0)
    val target2: NumericFuzzy[Double] = two.power(5.0)
    target1() shouldBe target2() +- 1E-10
    target1.fuzziness shouldBe target2.fuzziness +- 1E-10
    target1.isExact shouldBe target2.isExact
  }
  it should "work for x.power(-2.5).invert equals x.power(2.5)" in {
    val two = Bounded(2.0, 0.01)
    val target1: NumericFuzzy[Double] = two.power(-2.5).asInstanceOf[BaseNumericFuzzy[Double]].invert
    val target2: NumericFuzzy[Double] = two.power(2.5)
    target1() shouldBe target2() +- 1E-10
    target1.fuzziness shouldBe target2.fuzziness +- 1E-10
    target1.isExact shouldBe target2.isExact
  }
  it should "work for x.power(0.0) equals ONE" in {
    val two = Bounded(2.0, 0.01)
    val target1: NumericFuzzy[Double] = two.power(0.0)
    target1 shouldBe Exact(1.0)
  }
  // TODO test for Gaussian

  behavior of "exp"
  it should "work for exact" in {
    val one = Exact(1.0)
    val target = one.exp
    target() shouldBe math.E
    target.isExact shouldBe true
  }
  it should "work for bounded(1,..)" in {
    val one = Bounded(1.0, 1.0 / 10)
    val target = one.exp
    target() shouldBe math.E
    target.fuzziness shouldBe math.E / 10 +- 0.0000000000000001
    target.isExact shouldBe false
  }
  it should "work for bounded(2,...)" in {
    val two = Bounded(2.0, 1.0 / 10)
    val target = two.exp
    target() shouldBe math.E * math.E +- 0.000000000000001
    target.fuzziness shouldBe math.E * math.E / 10 +- 0.000000000000005
    target.isExact shouldBe false
  }
  // TODO test for Gaussian

}