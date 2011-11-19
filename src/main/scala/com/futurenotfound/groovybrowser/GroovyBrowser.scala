package com.futurenotfound.groovybrowser

import scala.swing._
import scala.swing.Swing._
import event.KeyPressed
import scala.swing.event.Key.Control
import scala.swing.event.Key.Enter
import scala.swing.event.Key.Modifier._
import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel
import javax.swing.UIManager
import java.awt.Color

object GroovyBrowser extends SimpleSwingApplication {
  UIManager.setLookAndFeel(new NimbusLookAndFeel());
  
  private val loader = new Loader()
  private val backButton = new Button{
    action = Action("Back") {
      loader.backward
    }
  }
  private val forwardButton = new Button{
    action = Action("Forward") {
      loader.forward
    }
  }
  private val editButton = new Button{
    action = Action("Edit") {
      swapEditorVisibility()
    }
  }
  private val locationTextField = new TextField{
    text = "http://www.futurenotfound.com/test2.groovy"
  }
  private val editorArea = new TextArea {}
  private val editorScrollPane = new ScrollPane {
    contents = editorArea
    visible = false
  }
  private val browserScrollPane = new ScrollPane
  private val splitPane = new SplitPane {
    leftComponent = editorScrollPane
    rightComponent = browserScrollPane
  }
  private val topPanel = new BoxPanel(Orientation.Horizontal) {
    contents += backButton
    contents += forwardButton
    contents += editButton
    contents += locationTextField
  }
  private val borderPanel = new BorderPanel {
    add(topPanel, BorderPanel.Position.North)
    add(splitPane, BorderPanel.Position.Center)
  }

  listenTo(loader, locationTextField.keys, editorArea.keys)
  reactions += {
    case KeyPressed(`locationTextField`, Enter, _, _) => loader.load(locationTextField.text)
    case KeyPressed(`editorArea`, Enter, Control, _) => loader.loadText(editorArea.text)
    case loadingURL: LoadingURLEvent => {
      locationTextField.background = Color.getHSBColor(140, 209, 230)
      locationTextField.text = loadingURL.url
    }
    case scriptLoaded: ScriptLoadedEvent => {
      locationTextField.background = Color.getHSBColor(75, 254, 120)
      editorArea.text = scriptLoaded.script
    }
    case componentLoaded: ComponentLoadedEvent => {
      locationTextField.background = Color.white
      browserScrollPane.viewportView = new Component {
        override lazy val peer = componentLoaded.component
      }
      componentLoaded.component.validate
    }
  }

  def top = new MainFrame {
    title = "Groovy Browser"
    contents = borderPanel
    size = (800, 600)
  }

  private def swapEditorVisibility() = {
    editorScrollPane.visible = !editorScrollPane.visible
    splitPane.dividerLocation = if (editorScrollPane.visible) 0.5 else 0
  }
}