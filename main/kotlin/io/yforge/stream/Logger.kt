package io.yforge.stream

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val lc: LoggerContext by lazy {
  val l = LoggerFactory.getILoggerFactory() as LoggerContext
  StatusPrinter.print(l)
  l
}

inline fun <reified T:Any> T.logger(): Logger {
  val k = this::class
  return lc.getLogger(
      if (k.isCompanion) k.java.enclosingClass.simpleName
      else k.java.simpleName)
}
