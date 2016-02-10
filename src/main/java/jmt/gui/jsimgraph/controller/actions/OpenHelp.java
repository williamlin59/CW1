/**    
  * Copyright (C) 2006, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

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

package jmt.gui.jsimgraph.controller.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import jmt.gui.jsimgraph.controller.Mediator;
import jmt.manual.ManualBookmarkers;
import jmt.manual.PDFViewerBuffer;

/**

 * @author Federico Granata
 * Date: 4-giu-2003
 * Time: 10.54.34

 */
public class OpenHelp extends AbstractJmodelAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Defines an <code>Action</code> object with a default
	 * description string and default icon.
	 */
	public OpenHelp(Mediator mediator) {
		super("JSIMgraph help", "JSIMgraph help", mediator);
		putValue(SHORT_DESCRIPTION, "Show JSIMgraph help");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
	}

	/**
	 * Invoked when an action occurs.
	 */
	public void actionPerformed(ActionEvent e) {
		//showHelp(e);

		Runnable r = new Runnable() {
			public void run() {
				try {
					new PDFViewerBuffer("JSIMgraph manual", ManualBookmarkers.JSIMgraph);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		EventQueue.invokeLater(r);

	}
}
