/*
 * LaScala
 * Copyright (c) 2016, 2017. Phasmid Software
 */

package com.phasmid.laScala

import scala.language.implicitConversions
import scala.util.{Success, Try}

/**
  * This trait defines an aspect of an object as being able to be rendered as a String.
  * The purposes of this alternative form of conversion to a String (in addition to the toString defined on Any),
  * are:
  * (1) to create a String explicitly for human-readability;
  * (2) to allow for (relative) indentation of complex data structures such as trees.
  *
  * Created by scalaprof on 1/7/17.
  */
trait Renderable {

  /**
    * Method to render this object in a human-legible manner.
    *
    * @param indent the number of "tabs" before output should start (when writing on a new line).
    * @param tab    an implicit function to translate the tab number (i.e. indent) to a String of white space.
    *               Typically (and by default) this will be uniform. But you're free to set up a series of tabs
    *               like on an old typewriter where the spacing is non-uniform.
    * @return a String that, if it has embedded newlines, will follow each newline with (possibly empty) white space,
    *         which is then followed by some human-legible rendition of *this*.
    */
  def render(indent: Int = 0)(implicit tab: Int => Prefix): String
}

case class RenderableTraversable(xs: Traversable[_]) extends Renderable {
  def render(indent: Int)(implicit tab: (Int) => Prefix): String = {
    def addString(b: StringBuilder, start: String, sep: String, end: String): StringBuilder = {
      var first = true

      b append start
      for (x <- xs) {
        if (first) {
          b append Renderable.renderElem(x, indent)
          first = false
        }
        else {
          b append sep
          b append Renderable.renderElem(x, indent)
        }
      }
      b append end

      b
    }

    addString(new StringBuilder, "(\n" + tab(indent + 1), ",\n" + tab(indent + 1), "\n" + tab(indent) + ")").toString()
  }
}

case class RenderableOption(xo: Option[_]) extends Renderable {

  def render(indent: Int)(implicit tab: (Int) => Prefix): String = xo match {
    case Some(x) => s"Some(" + Renderable.renderElem(x, indent) + ")"
    case _ => "None"
  }
}

case class RenderableTry(xy: Try[_]) extends Renderable {

  def render(indent: Int)(implicit tab: (Int) => Prefix): String = xy match {
    case Success(x) => s"Success(" + Renderable.renderElem(x, indent) + ")"
    case _ => "Failure"
  }
}

case class RenderableEither(e: Either[_, _]) extends Renderable {

  def render(indent: Int)(implicit tab: (Int) => Prefix): String = e match {
    case Left(x) => s"Left(" + Renderable.renderElem(x, indent) + ")"
    case Right(x) => s"Right(" + Renderable.renderElem(x, indent) + ")"
  }
}

object Renderable {
  implicit def renderableTraversable(xs: Traversable[_]): Renderable = RenderableTraversable(xs)

  implicit def renderableOption(xo: Option[_]): Renderable = RenderableOption(xo)

  implicit def renderableTry(xy: Try[_]): Renderable = RenderableTry(xy)

  implicit def renderableEither(e: Either[_, _]): Renderable = RenderableEither(e)

  def renderElem(elem: Any, indent: Int)(implicit tab: (Int) => Prefix): String = elem match {
    case xo: Option[_] => Renderable.renderableOption(xo).render(indent + 1)
    case xs: Traversable[_] => Renderable.renderableTraversable(xs).render(indent + 1)
    case xy: Try[_] => Renderable.renderableTry(xy).render(indent + 1)
    case e: Either[_, _] => Renderable.renderableEither(e).render(indent + 1)
    case r: Renderable => r.render(indent + 1)
    case x => x.toString
  }
}

case class Prefix(s: String) {
  override def toString: String = s
}

object Prefix {

  /**
    * The default tab method which translates indent uniformly into that number of double-spaces.
    *
    * @param indent the number of tabs before output should start on a new line.
    * @return a String of white space.
    */
  implicit def tab(indent: Int): Prefix = Prefix(" " * indent * 2)
}
