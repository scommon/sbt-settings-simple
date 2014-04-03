package org.scommon.sbt.settings

import java.net.URL
import sbt._
import scala.Some

object Utils {
  def readFileLines(file: java.io.File): Iterator[String] = {
    val source = io.Source.fromFile(file)
    source.getLines
  }

  def readFileFirstNonEmptyLine(file: java.io.File): Option[String] = {
    for(line <- readFileLines(file); trimmed = line.trim; if "" != trimmed)
      return Some(line)
    None
  }

  def readAllFileLines(file: java.io.File): String = {
    readFileLines(file) mkString "\n"
  }

  def optionUrl(proposal: String): Option[URL] =
    try Option(url(proposal))
    catch {
      case _: Throwable => None
    }

  def normalizeId(id: String): String =
    id.replaceAllLiterally("/", "-")
}