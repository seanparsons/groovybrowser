package com.futurenotfound.groovybrowser

import scala.io._
import javax.swing._
import javax.script. {ScriptEngine, ScriptEngineManager}
import java.util.concurrent. {Executors, ExecutorService}
import scala.collection.immutable._
import swing.Publisher
import swing.event.Event

case class ScriptLoadedEvent(val script: String) extends Event
case class ComponentLoadedEvent(val component: JComponent) extends Event
case class LoadingURLEvent(val url: String) extends Event

class Loader extends Publisher {
  private val executorService: ExecutorService = Executors.newSingleThreadExecutor
  private val factory = new ScriptEngineManager
  private var urls: List[String] = Nil
  private var position: Int = -1
  private val urlSyncObject: AnyRef = new AnyRef
  private var lastLoaded: String = ""

  def loadText(groovyText: String): Unit = {
    refreshContent(groovyText, null)
  }

  private def refreshContent(groovyText: String, url: String): Unit = {
    SwingUtilities.invokeLater(new Runnable {
      def run: Unit = {
        lastLoaded = groovyText
        publish(new ScriptLoadedEvent(lastLoaded))
        val newInnerComponent = evaluateScript(groovyText)
        publish(new ComponentLoadedEvent(newInnerComponent))
      }
    })
  }

  private def loadCurrent: Unit = {
    urlSyncObject synchronized {
      val url: String = urls(position)
      executorService.execute(new Runnable {
        def run: Unit = {
          val groovyText: String = readFromURL(url)
          refreshContent(groovyText, url)
        }
      })
    }
  }

  def getText(url: String): String = Source.fromURL(url).mkString

  private def readFromURL(url: String): String = {
    publish(new LoadingURLEvent(url))
    getText(url)
  }

  def getLastLoaded: String = {
    return lastLoaded
  }

  def load(url: String): Unit = {
    urlSyncObject synchronized {
      if (position == -1) {
        position = 0
      }
      else if (urls.last != url) {
        position += 1
        urls = urls.take(position)
      }
      if (urls.isEmpty || urls.last != url) {
        urls = urls :+ url
      }
      loadCurrent
    }
  }

  private def evaluateScript(groovyText: String): JComponent = {
    try {
      val engine: ScriptEngine = factory.getEngineByName("groovy")
      engine.put("loader", this)
      return engine.eval(groovyText).asInstanceOf[JComponent]
    }
    catch {
      case exception: Exception => {
        throw new RuntimeException("Exception thrown.", exception)
      }
    }
  }

  def forward: Unit = {
    urlSyncObject synchronized {
      if (position != -1 && position < urls.size - 1) {
        position += 1
        loadCurrent
      }
    }
  }

  def backward: Unit = {
    urlSyncObject synchronized {
      if (position > 0) {
        position -= 1
        loadCurrent
      }
    }
  }
}