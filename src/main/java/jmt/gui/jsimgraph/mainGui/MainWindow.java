/**
  * Copyright (C) 2012, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.

  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */

package jmt.gui.jsimgraph.mainGui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jmt.framework.gui.components.JMTFrame;
import jmt.framework.gui.components.JMTMenuBar;
import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.layouts.MultiBorderLayout;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.jsimgraph.controller.GraphMouseListner;
import jmt.gui.jsimgraph.controller.Mediator;

import org.jgraph.JGraph;

/**
 * MainWindow contains the main window of the jmodel project, it implements the
 * Singleton pattern. no need to create more then 1 main window!
 *

 * @author Federico Granata, Bertoli Marco
 * Date: 3-giu-2003
 * Time: 14.09.14

 */
public class MainWindow extends JMTFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String TITLE = "JSIMgraph - Advanced queueing network simulation tool";

	protected Mediator mediator;// mediator between components of the application
	protected JMTToolBar toolbar;//main toolbar
	protected JMTMenuBar menu;//main menu
	protected GraphMouseListner ml;//mouse listener of the JGraph
	protected JScrollPane scroll;//panel that contains the JGraph component

	protected JPanel mainPane;//panel that contains the "scroll"

	public static boolean advanced = false;

	/** Creates the new Main window of the application.
	 *
	 */
	public MainWindow() {
		super(true);
		this.setIconImage(JMTImageLoader.loadImage("JMODELIcon").getImage());
		setTitle(TITLE);
		mediator = new Mediator(null, this);
		Mediator.advanced = advanced;

		//menu = new Menu(mediator);
		menu = mediator.createMenu();
		setJMenuBar(menu);

		toolbar = mediator.createToolbar();
		getContentPane().setLayout(new MultiBorderLayout());
		getContentPane().add(toolbar, BorderLayout.NORTH);
		getContentPane().add(mediator.getComponentBar(), BorderLayout.NORTH);
		mainPane = new JPanel(new BorderLayout());
		getContentPane().add(mainPane, BorderLayout.CENTER);

		ml = new GraphMouseListner(mediator);
		mediator.setMouseListner(ml);

		scroll = new JScrollPane();
		mainPane.add(scroll, BorderLayout.CENTER);
		centerWindow(1024, 768);
		setVisible(true);
	}

	/** Sets the new Graph inside the scroll panel.
	 *
	 * @param newGraph
	 */
	public void setGraph(JGraph newGraph) {
		// 23/07/03 - Massimo Cattai //////////////////////////////////////////
		//removeEditor();
		mainPane.remove(scroll);
		// Old Code - remove(scroll);
		// 23/07/03 - end /////////////////////////////////////////////////////
		scroll = new JScrollPane(newGraph);
		// 23/07/03 - Massimo Cattai //////////////////////////////////////////
		mainPane.add(scroll, BorderLayout.CENTER);
		// Old Code - getContentPane().add(scroll);
		// 23/07/03 - end /////////////////////////////////////////////////////
		getContentPane().validate();
	}

	/* (non-Javadoc)
	 * @see jmt.framework.gui.components.JMTFrame#canBeClosed()
	 */
	@Override
	public boolean canBeClosed() {
		return !mediator.checkForSave("<html>Save changes before closing?</html>");
	}

	/* (non-Javadoc)
	 * @see jmt.framework.gui.components.JMTFrame#doClose()
	 */
	@Override
	protected void doClose() {
		// Ends simulation process if active
		mediator.stopSimulation();
		// Disposes resultsWindow (if present) and mainwindow
		if (mediator.getResultsWindow() != null) {
			mediator.getResultsWindow().dispose();
		}
		if (mediator.getPAProgressWindow() != null) {
			mediator.getPAProgressWindow().stopAnimation();
			mediator.getPAProgressWindow().dispose();
		}
	}

	/** Removes the current graph from the main window.
	 *
	 */
	public void removeGraph() {
		mainPane.remove(scroll);
		getContentPane().repaint();
	}

	public JScrollPane getScroll() {
		return scroll;
	}

	/** main function it activates the application .
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args != null && args.length > 0) && (args[0] != null) && (args[0].equals("trek"))) {
			advanced = true;
		}
		new MainWindow();
	}

	/**
	 * Updates this window title adding the file name
	 * @param filename the file name or null to remove it.
	 */
	public void updateTitle(String filename) {
		if (filename != null) {
			setTitle(TITLE + " - " + filename);
		} else {
			setTitle(TITLE);
		}
	}
}
