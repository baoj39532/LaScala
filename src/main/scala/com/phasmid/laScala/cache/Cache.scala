package com.phasmid.laScala.cache

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.{Failure, Success, Try}

/**
  * This trait defines a cache (aka memoization) of K,V key-value pairs.
  * It extends Function1[K,V].
  * If you call the apply method on the cache, it may throw an exception if it is unable to fulfill the value for the given key.
  *
  * @tparam K they key type
  * @tparam V the value type
  */
trait Cache[K, V, M[_]] extends (K => V) {

  //  /**
  //    * The type of the value returned by get. This could be Option[V], Try[V] or Future[V].
  //    */
  //  type T
  //
  /**
    * Method to get the value corresponding to key k as a T
    *
    * @param k the key we are interested in
    * @return the value wrapped as a T
    */
  def get(k: K): M[V]

  /**
    * Method to return the currently cached values as an immutable Map.
    * CONSIDER moving this method into the abstract class
    *
    * @return
    */
  def asMap: Map[K, V]

  /**
    * Method to determine if this cache contains the key k
    * CONSIDER moving this method into the abstract class or eliminating altogether since asMap.contains will yield the answer.
    *
    * @param k the key
    * @return true if this cache contains k else false
    */
  def contains(k: K): Boolean

  /**
    * Method to clear the cache such that there are absolutely no stale entries.
    * Calling clear does NOT refresh the cache as currently programmed.
    * Thus clear never causes the evaluationFunc method to be invoked.
    */
  def clear()
}

trait Expiring[K] {

  /**
    * method to remove a key from the cache
    *
    * @param k the key to the element which is to be expired.
    */
  def expire(k: K): Unit
}

/**
  * Basic self-fulfilling cache that uses Option
  *
  * @param evaluationFunc the evaluationFunc function of type K=>Option[V] which will get the V value when it is otherwise unknown.
  * @param carper         the function to carp about a problem, of type String=>Unit
  * @tparam K they key type
  * @tparam V the value type
  */
case class BasicFulfillingCache[K, V](evaluationFunc: K => Option[V])(implicit carper: String => Unit) extends SelfFulfillingExpiringCache[K, V](evaluationFunc)(carper) {
  /**
    * method to remove a key from the cache
    *
    * @param k the key to the element which is to be expired.
    */
  def expire(k: K) = {}

  /**
    * method to take action whenever a key value is actually fulfilled.
    *
    * @param k the key of the element which has been fulfilled
    * @param v the value of the element which has been fulfilled
    */
  def postFulfillment(k: K, v: V): Unit = {}
}

/**
  * Basic self-fulfilling cache that uses Try
  *
  * CONSIDER extending a different superclass since we don't want a cache that even has an expire method.
  *
  * @param evaluate the evaluationFunc function of type K=>Try[V] which will get the V value when it is otherwise unknown.
  * @param carper   the function to carp about a problem, of type String=>Unit
  * @tparam K they key type
  * @tparam V the value type
  */
case class NonExpiringCache[K, V](evaluate: K => Try[V])(implicit carper: String => Unit) extends TryFulfillingExpiringCache[K, V](evaluate)(carper) {
  /**
    * method to remove a key from the cache
    *
    * @param k the key to the element which is to be expired.
    */
  def expire(k: K): Unit = throw new UnsupportedOperationException("NonExpiringCache does not support expire")

  /**
    * method to take action whenever a key value is actually fulfilled.
    *
    * @param k the key of the element which has been fulfilled
    * @param v the value of the element which has been fulfilled
    */
  def postFulfillment(k: K, v: V): Unit = {}
}

/**
  * This abstract class implements a (mutable, obviously) Cache (aka Memoization) of K,V key-value pairs.
  * In particular, Cache is a lookup function of type K=>V.
  * If no value v for key k has been previously established,
  * the evaluationFunc method will be called when get(k) -- and so also when apply(k) -- is invoked.
  * Otherwise, the existing value will be returned.
  * If the result of calling evaluationFunc(k) is:
  * a Success(v) then v will be put into the internal hashmap and returned;
  * a Failure(x) then no entry will be put into the internal hashmap
  * (as a side-effect, the carper will be invoked for a failure)
  * In this way, a caller must be prepared to get None as the result of calling get(k) and must be prepared for
  * an exception to be thrown when calling apply(k).
  * The advantage is that we will only ever try to evaluationFunc a key at most once per expiration period.
  *
  * CONSIDER do we really need the carper method now that we return a Try[V] from apply?
  *
  * Created by scalaprof on 4/5/16.
  *
  * @param m      a CacheMap which is where the cached values will actually be stored
  * @param carper a method which processes a String and returns Unit
  * @tparam K they key type
  * @tparam V the value type
  * @tparam M a container (a monad) such as Try, Option, Future.
  */
abstract class AbstractExpiringCache[K, V, M[_]](m: CacheMap[K, V, M], evaluationFunc: K => M[V])(implicit carper: String => Unit) extends AbstractFulfillingCache[K, V, M](m, evaluationFunc)(carper) with Expiring[K]

/**
  * This abstract class implements a (mutable, obviously) Cache (aka Memoization) of K,V key-value pairs.
  * In particular, Cache is a lookup function of type K=>V.
  * If no value v for key k has been previously established,
  * the evaluationFunc method will be called when get(k) -- and so also when apply(k) -- is invoked.
  * Otherwise, the existing value will be returned.
  * If the result of calling evaluationFunc(k) is:
  * a Success(v) then v will be put into the internal hashmap and returned;
  * a Failure(x) then no entry will be put into the internal hashmap
  * (as a side-effect, the carper will be invoked for a failure)
  * In this way, a caller must be prepared to get None as the result of calling get(k) and must be prepared for
  * an exception to be thrown when calling apply(k).
  * The advantage is that we will only ever try to evaluationFunc a key at most once per expiration period.
  *
  * CONSIDER do we really need the carper method now that we return a Try[V] from apply?
  *
  * Created by scalaprof on 4/5/16.
  *
  * @param m      a CacheMap which is where the cached values will actually be stored
  * @param carper a method which processes a String and returns Unit
  * @tparam K they key type
  * @tparam V the value type
  * @tparam M a container (a monad) such as Try, Option, Future.
  */
abstract class AbstractFulfillingCache[K, V, M[_]](m: CacheMap[K, V, M], evaluationFunc: K => M[V])(implicit carper: String => Unit) extends Cache[K, V, M] with Mappish[K, V, M] with Fulfilling[K, V, M] {

  /**
    * Return a V value for the given K value
    *
    * @param k the K value we are interested in
    * @return the corresponding V value
    */
  def apply(k: K): V = unwrap(get(k))

  def mutableMap = m

  /**
    * Method to unwrap a value of type T.
    *
    * TODO figure out a way to avoid having to use asInstanceOf
    *
    * @param t the T value, which should be the same as an M[V] value
    * @return the unwrapped V value
    */
  def unwrap(t: M[V]): V = m.wrapper.get(t.asInstanceOf[M[V]])

  def evaluate(k: K): M[V] = evaluationFunc(k)

  override def toString = m.toString

}

/**
  * This class implements a (mutable, obviously) Cache (aka Memoization) of K,V key-value pairs.
  * In particular, Cache is a lookup function of type K=>V.
  * If no value v for key k has been previously established,
  * the evaluationFunc method will be called when get(k) -- and so also when apply(k) -- is invoked.
  * Otherwise, the existing value will be returned.
  * If the result of calling evaluationFunc(k) is:
  * a Some(v) then v will be put into the internal hashmap and returned;
  * a None then no entry will be put into the internal hashmap (and, as a side-effect, the carper will be invoked).
  * In this way, a caller must be prepared to get None as the result of calling get(k) and must be prepared for
  * an exception to be thrown when calling apply(k).
  * The advantage is that we will only ever try to evaluationFunc a key at most once per expiration period.
  *
  * SelfFulfillingExpiringCache defines two methods which must be implemented by subclasses:
  * expire (to remove an element from the cache) and postFulfillment (designed to allow a callback method
  * to be invoked when a value is added to the cache, i.e. it is fulfilled).
  *
  * Created by scalaprof on 4/5/16.
  *
  * @param evaluationFunc the method which, in the event of a key-value not being found, will result in an (optional) value
  * @param carper         a method which processes a String and returns Unit
  * @tparam K they key type
  * @tparam V the value type
  */
abstract class SelfFulfillingExpiringCache[K, V](evaluationFunc: K => Option[V])(implicit carper: String => Unit) extends AbstractExpiringCache[K, V, Option](new OptionHashMap[K, V](), evaluationFunc)(carper) {

  /**
    * Define T as Option[V]
    */
  type T = Option[V]

  /**
    * CONSIDER making this generic and putting in its own trait: Fulfilling
    * but note that it's tricky because T is known only to this class
    *
    * @param k the key of the element to be fulfilled
    * @return
    */
  def fulfill(k: K): T = evaluate(k) match {
    case Some(v) => postFulfillment(k, v); Some(v)
    case _ => carper(s"Cache: unable to evaluate value for key $k"); None
  }
}

/**
  * This class is almost exactly like SelfFulfillingExpiringCache but the get method returns a Try[V] rather than a Option[V].
  * There is a corresponding change in the definition of the evaluationFunc function (passed in as a parameter).
  *
  * Created by scalaprof on 4/5/16.
  *
  * @param evaluationFunc the method which, in the event of a key-value not being found, will result in an (optional) value
  * @param carper         a method which processes a String and returns Unit
  * @tparam K they key type
  * @tparam V the value type
  */
abstract class TryFulfillingExpiringCache[K, V](evaluationFunc: K => Try[V])(implicit carper: String => Unit) extends AbstractExpiringCache[K, V, Try](new TryHashMap[K, V](), evaluationFunc) {

  /**
    * Define T as Try[V]
    */
  type T = Try[V]

  /**
    *
    * @param k the key to be evaluated
    * @return a V wrapped in an M
    */
  def fulfill(k: K): T = evaluate(k).transform(
    { v => postFulfillment(k, v); Success(v) }, { case x => carper(s"Cache: evaluation exception for key $k: $x"); Failure(x) }
  )
}

/**
  * This class is almost exactly like SelfFulfillingExpiringCache but the get method returns a Future[V] rather than a Option[V].
  * There is a corresponding change in the definition of the evaluationFunc function (passed in as a parameter).
  *
  * Created by scalaprof on 4/5/16.
  *
  * @param evaluationFunc the method which, in the event of a key-value not being found, will result in an (optional) value
  * @param carper         a (implicit) method which processes a String and returns Unit
  * @param executor       an (implicit) execution context
  * @tparam K they key type
  * @tparam V the value type
  */
abstract class FutureFulfillingExpiringCache[K, V](evaluationFunc: K => Future[V])(implicit carper: String => Unit, executor: ExecutionContext) extends AbstractExpiringCache[K, V, Future](new FutureHashMap[K, V](), evaluationFunc) {
  /**
    * Define T as Future[V]
    */
  type T = Future[V]

  def fulfill(k: K): T = evaluate(k).transform(
    { v => postFulfillment(k, v); v }, { case x => carper(s"Cache: evaluation exception for key $k: $x"); x }
  )
}

object Cache {
  implicit def carper(s: String): Unit = println(s)
}

/**
  * This trait defines a Wrapper of values.
  *
  * @tparam M represents a container (which is a monad) that we wish to wrap our values in.
  */
trait Wrapper[M[_]] {
  /**
    * Return an M[V], given v
    *
    * @param v the value
    * @tparam V the value type
    * @return an M[V]
    */
  def unit[V](v: => V): M[V]

  /**
    * Return an M[V], given an M[V] and a V=>M[V] function
    *
    * CONSIDER definining this as flatMap[V,W] etc.
    *
    * @param m the M[V] to be mapped
    * @tparam V the value type
    * @return an M[V]
    */
  def flatMap[V](m: M[V], f: V => M[V]): M[V]

  /**
    * Unwrap a V value from inside the given m
    *
    * @param m an M[V]
    * @tparam V the value type
    * @return the unwrapped V value
    */
  def get[V](m: M[V]): V

  /**
    * Just for convenience, here is map defined in terms of flatMap and unit
    *
    * @param m the M[V] to be mapped
    * @param f the mapping function, which is a V=>V
    * @tparam V the value type
    * @return an M[V]
    */
  def map[V](m: M[V], f: V => V): M[V] = flatMap(m, v => unit(f(v)))
}

/**
  * A concrete Wrapper which wraps with a Future
  *
  * @param ec     an (implicit) execution context
  * @param atMost a (implicit) duration used by the get method
  */
class FutureWrapper(implicit ec: ExecutionContext, atMost: Duration) extends Wrapper[Future] {
  def flatMap[V](m: Future[V], f: V => Future[V]): Future[V] = m flatMap f

  def unit[V](v: => V): Future[V] = Future.successful(v)

  def get[V](m: Future[V]): V = Await.result(m, atMost)
}

/**
  * Companion object to Wrapper
  */
object Wrapper {
  val optionWrapper = new Wrapper[Option] {
    def flatMap[V](m: Option[V], f: V => Option[V]): Option[V] = m flatMap f

    def unit[V](v: => V): Option[V] = Some(v)

    def get[V](m: Option[V]): V = m.get
  }
  val tryWrapper = new Wrapper[Try] {
    def flatMap[V](m: Try[V], f: V => Try[V]): Try[V] = m flatMap f

    def unit[V](v: => V): Try[V] = Success(v)

    def get[V](m: Try[V]): V = m.get
  }
  // XXX The following may look unnecessary -- and will be removed by Organize Imports. But we do need it!
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  implicit val atMost: FiniteDuration = 1.second
  val futureWrapper = new FutureWrapper()
}

/**
  *
  * @param wrapper
  * @tparam K key type
  * @tparam V value type
  * @tparam M a container which is a monad, such as Option, Future, Try.
  */
abstract class CacheMap[K, V, M[_]](val wrapper: Wrapper[M]) extends mutable.HashMap[K, V] {
  self =>

  /**
    * This method, analagous to getOrElseUpdate returns the value (wrapped in an "M" container) corresponding to key k if it exists.
    * If it doesn't exist, it invokes the function f to get the value.
    *
    * @param k the key which references the value we need
    * @param f the function which is called to fulfill the value if it doesn't already exist
    * @return an M[V]
    */
  def getOrElseUpdateX(k: K, f: K => M[V]): M[V] = {
    def doUpdate(v: V): M[V] = {
      self.put(k, v); wrapper.unit(v)
    }
    get(k) match {
      case Some(v) => wrapper.unit(v)
      case None => wrapper.flatMap[V](f(k), doUpdate _)
    }
  }
}

/**
  * This class is a concrete subclass of CacheMap and uses Option as the container "M".
  *
  * @tparam K they key type
  * @tparam V the value type
  */
class OptionHashMap[K, V] extends CacheMap[K, V, Option](Wrapper.optionWrapper)

/**
  * This class is a concrete subclass of CacheMap and uses Try as the container "M".
  *
  * @tparam K they key type
  * @tparam V the value type
  */
class TryHashMap[K, V] extends CacheMap[K, V, Try](Wrapper.tryWrapper)

/**
  * This class is a concrete subclass of CacheMap and uses Future as the container "M".
  *
  * @tparam K they key type
  * @tparam V the value type
  */
class FutureHashMap[K, V] extends CacheMap[K, V, Future](Wrapper.futureWrapper)

/**
  * Trait to define the behavior of a cache as a map
  * Created by scalaprof on 7/6/16.
  *
  * @tparam K they key type
  * @tparam V the value type
  */
trait Mappish[K, V, M[_]] {

  // CONSIDER a different scope for this method
  private[cache] def mutableMap: CacheMap[K, V, M]

  def clear() = mutableMap.clear()

  def asMap = mutableMap.toMap

  def contains(k: K) = mutableMap.contains(k)
}

/**
  * Trait to define the behavior of a (self)-fulfilling cache.
  *
  * Created by scalaprof on 7/7/16.
  *
  * @tparam K they key type
  * @tparam V the value type
  */
trait Fulfilling[K, V, M[_]] extends Mappish[K, V, M] {

  /**
    * Non-blocking call to get the value v from the cache.
    *
    * @param k the key of the element which we wish to get
    * @return the value wrapped as a T
    */
  def get(k: K): M[V] = mutableMap.getOrElseUpdateX(k, fulfill _)

  /**
    * Fulfill the key k as an M[V]
    *
    * @param k the key to be evaluated
    * @return a V wrapped in an M
    */
  def fulfill(k: K): M[V]

  /**
    * Evaluate key k as an M[V]
    *
    * @param k the key to be evaluated
    * @return a V wrapped in an M
    */
  def evaluate(k: K): M[V]

  /**
    * method to take action whenever a key value is actually fulfilled.
    *
    * @param k the key of the element which has been fulfilled
    * @param v the value of the element which has been fulfilled
    */
  def postFulfillment(k: K, v: V): Unit
}


