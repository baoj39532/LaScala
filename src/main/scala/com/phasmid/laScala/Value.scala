package com.phasmid.laScala

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.phasmid.laScala.Orderable.OrderableDate
import com.phasmid.laScala.parser.Valuable

import scala.util._

/**
  * Trait Value.
  *
  * The purpose of this trait is to be able to represent quantities -- things that have some ordered (and/or numerical)
  * value -- (or sequences of quantities) in a more meaningful way than simply using "Any".
  *
  * This trait defines five methods: source, asBoolean, asValuable, asOrderable, asSequence.
  *
  * For values that you want to consider as numeric, then use asValuable. Valuable is very similar to Numeric
  * but has additional methods.
  *
  * For values that you want to consider as orderable, then use asOrderable. Orderable extends Ordering.
  * It is used for the type of quantities that do not support arithmetic operations, but do support ordering.
  * A prime example is Date, Datetime, etc.
  *
  * You can also represent sequences by Value (in particular, SequenceValue). Such values will yield Some(sequence)
  * when asSequence is invoked. Other types of Value will yield None in this situation.
  *
  * In the vast majority of cases, you can simply provide a String as the input to Value.apply.
  * This is the normal mechanism when you are reading values from HTML, JSON, Config, whatever.
  * Values that derive from String representations are normally StringValues
  * These typically result in an appropriate value when asValuable is invoked, that's to say --
  * not the String value but the parsed numeric (Valuable) value.
  * If you have a String that you want only to be considered a String, then use QuotedStringValue
  *
  * Created by scalaprof on 7/8/16.
  */
sealed trait Value {

  /**
    * If this Value is a Boolean, then return Some(true) or Some(false) as appropriate.
    * Otherwise, return None.
    *
    * @return Some(true), Some(false) or None
    */
  def asBoolean: Option[Boolean]

  /**
    * Transform this Value into an (optional) X value which is Orderable
    *
    * @tparam X the type of the result we desire
    * @return either Some(x) if this value can be represented so, without information loss;
    *         or None otherwise.
    */
  def asOrderable[X: Orderable](implicit pattern: String): Option[X]

  /**
    * Transform this Value into an (optional) X value which is Valuable
    *
    * @tparam X the type of the result we desire
    * @return either Some(x) if this value can be represented so, without information loss;
    *         or None otherwise.
    */
  def asValuable[X: Valuable]: Option[X]

  /**
    * View this Value as a Sequence of Value objects.
    *
    * @return either Some(sequence) or None, as appropriate.
    */
  def asSequence: Option[Seq[Value]]

  def source: Any
}

/**
  * Value which is natively an Int. Such a value can be converted to Double by invoking asValuable[Double]
  *
  * @param x      the Int value
  * @param source the source (which could, conceivably, be a String)
  */
case class IntValue(x: Int, source: Any) extends Value {
  def asBoolean: Option[Boolean] = None

  def asValuable[X: Valuable]: Option[X] = implicitly[Valuable[X]].fromInt(x).toOption

  def asOrderable[X: Orderable](implicit pattern: String = ""): Option[X] = None

  def asSequence: Option[Seq[Value]] = None

  override def toString = x.toString
}

/**
  * Value which is natively a Boolean. Such a value can be converted to Int by invoking asValuable[Int] in which case
  * the result will be 1 for true and 0 for false.
  *
  * @param x      the Int value
  * @param source the source (which could, conceivably, be a String)
  */
case class BooleanValue(x: Boolean, source: Any) extends Value {
  def asBoolean: Option[Boolean] = Some(x)

  def asValuable[X: Valuable]: Option[X] = implicitly[Valuable[X]].fromInt(if (x) 1 else 0).toOption

  def asOrderable[X: Orderable](implicit pattern: String = ""): Option[X] = None

  def asSequence: Option[Seq[Value]] = None

  override def toString = x.toString
}

/**
  * Value which is natively a Double. Such a value cannot be converted to Int by invoking asValuable[Int] because of
  * loss of precision.
  *
  * @param x      the Double value
  * @param source the source (which could, conceivably, be a String)
  */
case class DoubleValue(x: Double, source: Any) extends Value {
  def asBoolean: Option[Boolean] = None

  // XXX this gives us the effect we want -- conversion to Double but not, e.g. Int.
  // However, it is not elegant.
  // We really should try to convert a Double to an Int, for example, and test there is no information loss.
  def asValuable[X: Valuable]: Option[X] = Try(implicitly[Valuable[X]].unit(x.asInstanceOf[X])).toOption

  def asOrderable[X: Orderable](implicit pattern: String = ""): Option[X] = None

  def asSequence: Option[Seq[Value]] = None

  override def toString = x.toString
}

/**
  * Value which is natively a String. Such a value, providing it is formatted appropriately, can be converted to Int or
  * Double by invoking asValuable[Int] or asValuable[Double], respectively.
  *
  * @param x      the String value
  * @param source the source (normally a String)
  */
case class StringValue(x: String, source: Any) extends Value {
  def asBoolean: Option[Boolean] = None

  def asValuable[X: Valuable]: Option[X] = implicitly[Valuable[X]].fromString(x)("").toOption

  def asOrderable[X: Orderable](implicit pattern: String): Option[X] = implicitly[Orderable[X]].fromString(x)(pattern).toOption

  def asSequence: Option[Seq[Value]] = None

  override def toString = x
}

/**
  * Value which is natively a String. Such a value cannot be converted to Int or
  * Double by invoking asValuable.
  *
  * @param x      the String value
  * @param source the source (normally a String but might be enclosed in quotation marks)
  */
case class QuotedStringValue(x: String, source: Any) extends Value {
  def asBoolean: Option[Boolean] = None

  def asValuable[X: Valuable]: Option[X] = None

  // TODO create a concrete implicit object for OrderableString
  def asOrderable[X: Orderable](implicit pattern: String = ""): Option[X] = Try(implicitly[Orderable[X]].unit(x.asInstanceOf[X])).toOption

  // CONSIDER creating a sequence of Char?
  def asSequence: Option[Seq[Value]] = None

  override def toString = source.toString
}

/**
  * Value which is natively an LocalDate. Such a value, cannot be converted to Int or
  * Double by invoking asValuable. However, it can be converted to some other form of Date by invoking
  * asOrderable[X] where X is the other form--provided that there is an implict conversion function in scope.
  *
  * @param x      the LocalDate value
  * @param source the source (normally a String)
  */
case class DateValue(x: LocalDate, source: Any) extends Value {
  def asBoolean: Option[Boolean] = None

  def asValuable[X: Valuable]: Option[X] = None

  def asOrderable[X: Orderable](implicit pattern: String): Option[X] = Try(implicitly[Orderable[X]].unit(x.asInstanceOf[X])).toOption

  def asSequence: Option[Seq[Value]] = None

  override def toString = source.toString
}

/**
  * Value which is actually a Seq of Values. Such a value, cannot be converted to Int or
  * Double by invoking asValuable, nor by invoking asOrderable. However, when tested by invoking asSequence, such values
  * result in Some(sequence).
  *
  * @param xs     the Seq of Values
  * @param source the source (typically, a Seq of Any objects)
  */
case class SequenceValue(xs: Seq[Value], source: Any) extends Value {
  def asBoolean: Option[Boolean] = None

  def asOrderable[X: Orderable](implicit pattern: String): Option[X] = None

  def asValuable[X: Valuable]: Option[X] = None

  def asSequence: Option[Seq[Value]] = Some(xs)

  override def toString = xs.toString
}

class ValueException(s: String, t: scala.Throwable = null) extends Exception(s,t)

object BooleanValue {
  def apply(x: Boolean): BooleanValue = BooleanValue(x, x)
}

object IntValue {
  def apply(x: Int): IntValue = IntValue(x, x)
}

object DoubleValue {
  def apply(x: Double): DoubleValue = DoubleValue(x, x)
}

object StringValue {
  def apply(x: String): StringValue = StringValue(x, x)
}

object QuotedStringValue {
  def apply(x: String): QuotedStringValue = QuotedStringValue(x, x)
}

object DateValue {
  def apply(x: LocalDate): DateValue = DateValue(x, x)

  def apply(x: String)(implicit pattern: String): DateValue = DateValue(LocalDate.parse(x, if (pattern.isEmpty) DateTimeFormatter.ISO_LOCAL_DATE else OrderableDate.formatter(pattern)),x)

  def apply(y: Int, m: Int, d: Int): DateValue = apply(LocalDate.of(y, m, d))
}

object SequenceValue {
  def apply(xs: Seq[Any]): SequenceValue = {
    SequenceValue(Value.sequence(xs),xs)
  }
}

object Value {

  implicit def apply(x: Boolean): Value = BooleanValue(x)

  implicit def apply(x: Int): Value = IntValue(x)

  implicit def apply(x: Double): Value = DoubleValue(x, x)

  implicit def apply(x: String): Value = x match {
    case quoted(z) => QuotedStringValue(z, x)
    case _ => StringValue(x, x)
  }

  implicit def apply(x: LocalDate): Value = DateValue(x, x)

  /**
    * Method to convert any of several types of object into a Value
    *
    * @param x an Any
    * @return a Try[Value] where the value is the result of applying one of the several apply methods in this Value object.
    */
  def tryValue(x: Any): Try[Value] = x match {
    case b: Boolean => Try(apply(b))
    case i: Int => Try(apply(i))
    case d: Double => Try(apply(d))
    case w: String => Try(apply(w))
    case d: LocalDate => Try(apply(d))
    // XXX shouldn't really need the following...
    case v: Value => Success(v)
    case _ => Failure(new ValueException(s"cannot form Value from type ${x.getClass}"))
  }

  /**
    * Transform a sequence of Strings into a Sequence of corresponding Values
    *
    * @param ws a sequence of Strings
    * @return a sequence of Values
    */
  def sequence(ws: Seq[Any]): Seq[Value] = {
    FP.sequence(ws map {
      tryValue(_)
    }) match {
      case Success(as) => as
      case Failure(x) => throw new ValueException(s"cannot form sequence of Values from given sequence $ws", x)
    }
  }

  /**
    * Transform a Map of Strings into a Map of corresponding Values.
    *
    * XXX note that there is no native Value which is a Map.
    *
    * @param kWm a map of Strings
    * @tparam K the key type
    * @return a map of Values
    */
  def sequence[K](kWm: Map[K, Any]): Map[K, Value] = {
    val vtKs = (for ((k, v) <- kWm) yield (k, tryValue(v))).toSeq
    FP.sequence(for ((k, vt) <- vtKs) yield for (v <- vt) yield (k, v)) match {
      case Success(m) => m.toMap
      case Failure(x) => throw new ValueException(s"cannot form sequence of Values from given sequence", x)
    }
  }

  /**
    * Transform a sequence of Strings into a Sequence of corresponding Values
    *
    * @param ws a sequence of Strings
    * @return a sequence of Values
    */
  def trySequence(ws: Seq[Any]): Try[Seq[Value]] = FP.sequence(ws map {
    Value.tryValue(_)})

  /**
    * Transform a Map of Strings into a Map of corresponding Values
    *
    * @param kWm a map of Strings
    * @tparam K the key type
    * @return a map of Values
    */
  def trySequence[K](kWm: Map[K, Any]): Try[Map[K, Value]] = for (
    kVs <- FP.sequence((for ((k,v) <- kWm) yield for (z <- Value.tryValue(v)) yield (k, z)).toSeq)
  ) yield kVs.toMap

  val quoted = """"([^"]*)"""".r
}


