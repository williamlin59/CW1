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

package jmt.jmva.gui.exact;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import jmt.common.exception.InputDataException;
import jmt.common.exception.SolverException;
import jmt.framework.gui.components.JMTMenuBar;
import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.listeners.AbstractJMTAction;
import jmt.framework.gui.listeners.MenuAction;
import jmt.framework.gui.wizard.Wizard;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.definitions.ModelConverter;
import jmt.gui.common.panels.AboutDialogFactory;
import jmt.gui.common.panels.WarningWindow;
import jmt.gui.common.xml.ModelLoader;
import jmt.gui.jsimwiz.JSIMMain;
import jmt.gui.jsimwiz.definitions.JSIMModel;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.SolverAlgorithm;
import jmt.jmva.gui.exact.link.SolverClient;
import jmt.jmva.gui.exact.panels.AMVAPanel;
import jmt.jmva.gui.exact.panels.AlgorithmPanel;
import jmt.jmva.gui.exact.panels.ClassesPanel;
import jmt.jmva.gui.exact.panels.DescriptionPanel;
import jmt.jmva.gui.exact.panels.ForceUpdatablePanel;
import jmt.jmva.gui.exact.panels.GraphPanel;
import jmt.jmva.gui.exact.panels.IterationSelector;
import jmt.jmva.gui.exact.panels.QueueLenPanel;
import jmt.jmva.gui.exact.panels.ReferenceStationPanel;
import jmt.jmva.gui.exact.panels.ResTimePanel;
import jmt.jmva.gui.exact.panels.ServiceDemandsPanel;
import jmt.jmva.gui.exact.panels.ServiceTimesPanel;
import jmt.jmva.gui.exact.panels.StationsPanel;
import jmt.jmva.gui.exact.panels.SynopsisPanel;
import jmt.jmva.gui.exact.panels.SysPowerPanel;
import jmt.jmva.gui.exact.panels.SystemRespTimePanel;
import jmt.jmva.gui.exact.panels.ThroughputPanel;
import jmt.jmva.gui.exact.panels.UtilizationPanel;
import jmt.jmva.gui.exact.panels.VisitsPanel;
import jmt.jmva.gui.exact.panels.WhatIfPanel;
import jmt.manual.ManualBookmarkers;
import jmt.manual.PDFViewerBuffer;

/**
 * This is the object you use to define your system structure and parameters
 * @author alyf (Andrea Conti)
 * @version Date: 11-set-2003 Time: 14.47.11
 *
 * Modified by Bertoli Marco 01-mar-2006 (added model conversion and solved bug
 * with stored visits)
 *
 *Modified by Kourosh OCT2013 to add New panels such as ReferenceStation and SystemResponseTime
 *Also there is a modification to show the initial page, now at the beginning we see visits and service times and if the user press on Service Demand we
 *have a warning message which shows that by pressing this button visits are set to 1. 
 */
public class ExactWizard extends Wizard {
	private static final long serialVersionUID = 1L;
	private static final String TITLE = "JMVA - Product form queueing network solver";
	private final static boolean DEBUG = false;

	private ExactModel data;
	private JLabel helpLabel;
	private HoverHelp help;
	private SolverClient solver;

	// New Bertoli Marco
	private ModelLoader modelLoader = new ModelLoader(ModelLoader.JMVA);
	// End

	//NEW Dall'Orso
	//A link to the last modified model's temporary file - used to display synopsis
	private File tempFile = null;
	//END
	
	//keep a reference to these three components to enable switching
	private WizardPanel serviceTimesPanel;
	private WizardPanel serviceDemandsPanel;
	private WizardPanel visitsPanel;
	private WizardPanel whatIfPanel;
	private WizardPanel ReferenceStationPanel;
	private AMVAPanel amvaPanel;
	

	private AbstractJMTAction FILE_SAVE = new AbstractJMTAction("Save...") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Save Model");
			setIcon("Save", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		}

		public void actionPerformed(ActionEvent e) {
			save();
		}
	};

	private AbstractJMTAction FILE_OPEN = new AbstractJMTAction("Open...") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Open Saved Model");
			setIcon("Open", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		}

		public void actionPerformed(ActionEvent e) {
			open();
		}
	};

	private AbstractJMTAction FILE_NEW = new AbstractJMTAction("New...") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Create New Model");
			setIcon("New", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		}

		public void actionPerformed(ActionEvent e) {
			newModel();
		}
	};

	private AbstractJMTAction FILE_EXIT = new AbstractJMTAction("Exit") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Exits Application");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_Q));
		}

		public void actionPerformed(ActionEvent e) {
			close();
		}
	};

	private AbstractJMTAction SWITCH_TO_SIMULATOR = new AbstractJMTAction("Import in JSIM...") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Import current model into JSIMwiz...");
			setIcon("toJSIM", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
		}

		public void actionPerformed(ActionEvent e) {
			switchToSimulator();
		}
	};

	private AbstractJMTAction ACTION_RANDOMIZE_MODEL = new AbstractJMTAction("Randomize") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Random generation of service demands");
			setIcon("dice", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		}

		public void actionPerformed(ActionEvent e) {
			randomizeModel();
		}
	};

	private AbstractJMTAction HELP = new AbstractJMTAction("JMVA help") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Show JMVA help");
			setIcon("Help", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		}

		public void actionPerformed(ActionEvent e) {
			//showHelp(e);

			Runnable r = new Runnable() {
				public void run() {
					try {
						new PDFViewerBuffer("JMVA manual", ManualBookmarkers.JMVA);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			EventQueue.invokeLater(r);

		}
	};

	private AbstractJMTAction ABOUT = new AbstractJMTAction("About JMVA...") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "About JMVA");
			/*
			putValue(Action.SMALL_ICON, ImageLoader.loadImage("helpIcon"));
			putValue(Action.ACCELERATOR_KEY,
			        KeyStroke.getKeyStroke(KeyEvent.VK_H,
			                ActionEvent.ALT_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
			*/
		}

		public void actionPerformed(ActionEvent e) {
			showAbout(false);
		}
	};

	private AbstractJMTAction ACTION_SOLVE = new AbstractJMTAction("Solve") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Solve model");
			setIcon("Sim", JMTImageLoader.getImageLoader());

			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));

		}

		public void actionPerformed(ActionEvent e) {

			if (checkFinish()) {
				finish();
			}

		}
	};
	

	public ExactWizard() {
		this(new ExactModel());
	}

	public ExactWizard(ExactModel data) {
		super(TITLE);
		setSize(1024, 768);
		this.centerWindow();
		setIconImage(JMTImageLoader.loadImageAwt("JMVAIcon"));
		this.data = data;
		data.resetChanged();
		this.setJMenuBar(makeMenubar());
		getContentPane().add(makeToolbar(), BorderLayout.NORTH);
		addPanel(new ClassesPanel(this));
		addPanel(new StationsPanel(this));
		serviceTimesPanel = new ServiceTimesPanel(this);
		visitsPanel = new VisitsPanel(this);
		
		serviceDemandsPanel = new ServiceDemandsPanel(this);
		if (data.areVisitsSet()) {
			
			addPanel(serviceDemandsPanel);
		} else {
			addPanel(serviceTimesPanel);
			addPanel(visitsPanel);
			
		}
		
		ReferenceStationPanel = new ReferenceStationPanel(this);
		addPanel(ReferenceStationPanel);
		
		
		/* EDITED by Abhimanyu Chugh */
		whatIfPanel = new WhatIfPanel(this);
		addPanel(whatIfPanel);
		addPanel(new DescriptionPanel(this));
	
		setVisible(true);
		/* END */
	}

	/**
	 * @return the toolbar for the exact wizard. Shamelessly uses icon from the main jmt frame
	 */
	protected JMTToolBar makeToolbar() {

		JMTToolBar tb = new JMTToolBar(JMTImageLoader.getImageLoader());
		tb.setFloatable(false);

		//null values add a gap between toolbar icons
		AbstractJMTAction[] actions = { FILE_NEW, FILE_OPEN, FILE_SAVE, null, ACTION_SOLVE, SWITCH_TO_SIMULATOR, ACTION_RANDOMIZE_MODEL, null, HELP,null };
		String[] htext = { "Creates a new model", "Opens a saved model", "Saves the current model", "Solves the current model",
				"Import current model to JSIMwiz to solve it with the simulator", "Randomize model data", "Show help"};
		ArrayList<AbstractButton> buttons = new ArrayList<AbstractButton>();
		buttons.addAll(tb.populateToolbar(actions));
		
		//adds the algorithm selection box

		amvaPanel = new AMVAPanel(this);
		tb.add(amvaPanel);
		
		// Adds help
		for (int i = 0; i < buttons.size(); i++) {
			AbstractButton button = buttons.get(i);
			help.addHelp(button, htext[i]);
		}
		return tb;
	}


	private JMTMenuBar makeMenubar() {
		JMTMenuBar jmb = new JMTMenuBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] menuItems = new AbstractJMTAction[] {
				new MenuAction("File", new AbstractJMTAction[] { FILE_NEW, FILE_OPEN, FILE_SAVE, null, FILE_EXIT }),
				new MenuAction("Action", new AbstractJMTAction[] { ACTION_SOLVE, ACTION_RANDOMIZE_MODEL, null, SWITCH_TO_SIMULATOR, null,
						ACTION_NEXT, ACTION_PREV }), new MenuAction("Help", new AbstractJMTAction[] { HELP, null, ABOUT }), };

		jmb.populateMenu(menuItems);
		return jmb;
	}

	/**
	 * @return the button panel
	 */
	@Override
	protected JComponent makeButtons() {
		help = new HoverHelp();
		helpLabel = help.getHelpLabel();

		helpLabel.setBorder(BorderFactory.createEtchedBorder());
		//helpLabel.setHorizontalAlignment(SwingConstants.CENTER);

		ACTION_FINISH.putValue(Action.NAME, "Solve");
		ACTION_CANCEL.putValue(Action.NAME, "Exit");

		JPanel buttons = new JPanel();

		JButton button_finish = new JButton(ACTION_FINISH);
		help.addHelp(button_finish, "Validates the system and starts the solver");
		JButton button_cancel = new JButton(ACTION_CANCEL);
		help.addHelp(button_cancel, "Exits the wizard discarding all changes");
		JButton button_next = new JButton(ACTION_NEXT);
		help.addHelp(button_next, "Moves on to the next step");
		JButton button_previous = new JButton(ACTION_PREV);
		help.addHelp(button_previous, "Goes back to the previous step");
		buttons.add(button_previous);
		buttons.add(button_next);
		buttons.add(button_finish);
		buttons.add(button_cancel);

		JPanel labelbox = new JPanel();
		labelbox.setLayout(new BorderLayout());
		labelbox.add(Box.createVerticalStrut(30), BorderLayout.WEST);
		labelbox.add(helpLabel, BorderLayout.CENTER);

		Box buttonBox = Box.createVerticalBox();
		buttonBox.add(buttons);
		buttonBox.add(labelbox);
		return buttonBox;
	}

	//BEGIN Federico Dall'Orso 8/3/2005
	//NEW
	private void newModel() {
		currentPanel.lostFocus();
		if (checkForSave("<html>Save changes before creating a new model?</html>")) {
			return;
		}
		Rectangle bounds = this.getBounds();
		ExactWizard ew = new ExactWizard();
		updateTile(null);
		ew.setBounds(bounds);
		ew.setVisible(true);
		this.setVisible(false);
		this.dispose();
	}

	//END Federico Dall'Orso 8/3/2005

	/**
	 * Shows a confirmation dialog to save before new model or exit operations
	 * @param msg The message to display.
	 * @return <code>true</code> - if the user select cancel button.
	 */
	public boolean checkForSave(String msg) {
		// Checks if there's an old graph to save
		if (data != null && data.isChanged()) {
			int resultValue = JOptionPane.showConfirmDialog(this, msg, "JMVA - Warning", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);
			if (resultValue == JOptionPane.YES_OPTION) {
				save();
				return true;
			}
			if (resultValue == JOptionPane.CANCEL_OPTION) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Saves current model
	 * <br>Author: Bertoli Marco
	 */
	private void save() {
		currentPanel.lostFocus();
		if (!checkFinish()) {
			return; // panels with problems are expected to notify the user by themselves
		}
		int retval = modelLoader.saveModel(data, this, null);
		switch (retval) {
			case ModelLoader.SUCCESS:
				data.resetChanged();
				updateTile(modelLoader.getSelectedFile().getName());
				break;
			case ModelLoader.FAILURE:
				JOptionPane.showMessageDialog(this, modelLoader.getFailureMotivation(), "Error", JOptionPane.ERROR_MESSAGE);
				break;
		}
	}

	/**
	 * Opens a new model
	 * <br>Author: Bertoli Marco
	 */
	private void open() {
		currentPanel.lostFocus();
		if (checkForSave("<html>Save changes before opening a saved model?</html>")) {
			return;
		}
		ExactModel newdata = new ExactModel();
		int retval = modelLoader.loadModel(newdata, this);
		switch (retval) {
			case ModelLoader.SUCCESS:
			case ModelLoader.WARNING:
				data = newdata;
				currentPanel.gotFocus();
				// Shows right panels  //Edited by Kourosh, because we need to show Visits and Service Demands as Defaults.
				if (data.areVisitsSet()) {
					removePanel(visitsPanel);
					removePanel(serviceTimesPanel);
					((ForceUpdatablePanel) serviceDemandsPanel).retrieveData();
					addPanel(serviceDemandsPanel, 2);
				} else {
					removePanel(serviceDemandsPanel);
					((ForceUpdatablePanel) serviceTimesPanel).retrieveData();
					((ForceUpdatablePanel) visitsPanel).retrieveData();
					addPanel(visitsPanel, 2);
					addPanel(serviceTimesPanel, 2);									
				}
				updateTile(modelLoader.getSelectedFile().getName());
				tabbedPane.setSelectedIndex(0);
				break;
			case ModelLoader.FAILURE:
				JOptionPane.showMessageDialog(this, modelLoader.getFailureMotivation(), "Error", JOptionPane.ERROR_MESSAGE);
				break;

		}
		tempFile = modelLoader.getSelectedFile();
		if (data.hasResults()) {
			this.createSolutionWindow();
		}
		updatePanels();

		// Shows warnings if any
		if (retval == ModelLoader.WARNING) {
			new WarningWindow(modelLoader.getLastWarnings(), this, modelLoader.getInputFileFormat(), CommonConstants.JMVA).show();
		}
	}

	public ExactModel getData() {
		return data;
	}

	@Override
	protected void finish() {
		//OLD
		//do not call this method!!! it is already called inside checkFinish() method.
		//currentPanel.lostFocus();

		solve();
	}

	@Override
	protected boolean cancel() {
		if (currentPanel != null) {
			currentPanel.lostFocus();
		}
		return !checkForSave("<html>Save changes before closing?</html>");
	}

	protected void switchToSimulator() {
		JSIMModel output = new JSIMModel();
		// New Converter by Bertoli Marco
		List<String> res = ModelConverter.convertJMVAtoJSIM(data, output);
		JSIMMain jsim = new JSIMMain(output);
		jsim.setVisible(true);
		// If problems are found, shows warnings
		if (res.size() > 0) {
			new WarningWindow(res, jsim, CommonConstants.JMVA, CommonConstants.JSIM).show();
		}

	}

	public HoverHelp getHelp() {
		return help;
	}

	/**switches service times and visits panels to service demands panel in order to change
	 * data representation.*/ //Edited by Kourosh, because we need to show Visits and Service Demands as Defaults.
	public void switchFromSTVtoSD() {
		//kourosh123
		int resultValue = JOptionPane.showConfirmDialog(this, "Visit values will be set to \"1\" Continue?", "JMVA - Warning", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (resultValue == JOptionPane.YES_OPTION) {			
			((ForceUpdatablePanel) serviceTimesPanel).commitData();
			((ForceUpdatablePanel) visitsPanel).retrieveData();
			((ForceUpdatablePanel) visitsPanel).commitData();
			removePanel(serviceTimesPanel);
			removePanel(visitsPanel);
			((ForceUpdatablePanel) serviceDemandsPanel).retrieveData();
			addPanel(serviceDemandsPanel, 2);
			
		}
		if (resultValue == JOptionPane.CANCEL_OPTION) {
			
		}
		
		
	}

	/**switches service times and visits panels to service demands panel in order to change
	 * data representation.*/
	public void switchFromSDtoSTV() {
		((ForceUpdatablePanel) serviceDemandsPanel).commitData();
		removePanel(serviceDemandsPanel);
		((ForceUpdatablePanel) serviceTimesPanel).retrieveData();
		((ForceUpdatablePanel) visitsPanel).retrieveData();
		addPanel(visitsPanel, 2);
		addPanel(serviceTimesPanel, 2);
	}

	private void solve() {

		if (solver == null) {
			solver = new SolverClient(this);
		}

		ExactModel newdata = new ExactModel(data); // Yields the mean performance indices
		
		// Checks saturation
		int state = data.checkSaturation();
		switch (state) {
			case ExactModel.SATURATION:
				JOptionPane.showMessageDialog(this, "Error: input data will cause model saturation. Please adjust arrival rates or service demands.",
						"Input data error", JOptionPane.ERROR_MESSAGE);
				return;
			case ExactModel.SATURATION_WHATIF:
				JOptionPane.showMessageDialog(this,
						"Error: input data will cause model saturation during what-if analysis. Please adjust what-if analysis parameters.",
						"Input data error", JOptionPane.ERROR_MESSAGE);
				return;
		}
		
		// Checks reference station consistency
		boolean bool = data.checkVisitReferenceStation();
		if(bool==false){
			JOptionPane.showMessageDialog(this, "Error: for all classes the visit of the reference station must be different from zero. Please adjust visit values or change the reference station.",
					"Input data error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		bool = data.checkClassesReferenceStation();
		if(bool==false){
			JOptionPane.showMessageDialog(this, "Error: the reference station must be the same for all closed classes. Please adjust the reference station classes.",
					"Input data error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		newdata.resetResults();
		/* END */
		
		try {
			//OLD
			/*
			solver.solve(newdata);
			*/
			//NEW Dall'Orso
			tempFile = solver.solve(newdata);
			//OLD
			//NEW
			//@author Stefano Omini
		} catch (InputDataException rse) {
			JOptionPane.showMessageDialog(this, rse.getMessage(), "Input data error", JOptionPane.ERROR_MESSAGE);
			return;
			//end NEW
		} catch (SolverException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Solver error", JOptionPane.ERROR_MESSAGE);
			return;
		} catch (OutOfMemoryError e) {
			JOptionPane.showMessageDialog(this, "Out of memory error. Try to run Java Virtual Machine with more heap size (-Xmx<num>m)",
					"Out of Memory", JOptionPane.ERROR_MESSAGE);
			return;
		}
		this.data = newdata;
		if (data.hasResults()) {
			createSolutionWindow();
		}
		updatePanels();
		currentPanel.gotFocus();
	}
	
	//@author Bertoli Marco
	private void showAbout(boolean autoclose) {
		AboutDialogFactory.showJMVA(this, autoclose);
	}

	//NEW Dall'Orso
	private void createSolutionWindow() {
		JTabbedPane jtp = new JTabbedPane();
		String resultTitle = "JMVA Solutions";
		if (!data.isWhatifAlgorithms()) {
			resultTitle += " - " + data.getAlgorithmType().toString();
		}
		JFrame solutionWindow = new JFrame(resultTitle);
		solutionWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		solutionWindow.getContentPane().add(jtp);
		solutionWindow.setIconImage(this.getIconImage());
		IterationSelector selector = null;
		if (data.isWhatIf()) {
			// Graphic panel (Bertoli Marco)
			jtp.add(new GraphPanel(data));
			selector = new IterationSelector(data);
		}
		/* EDITED by Abhimanyu Chugh */
		if (selector != null && data.isClosed() && data.isWhatifAlgorithms()) {
			for (SolverAlgorithm algorithm : data.getWhatifAlgorithms()) {
				AlgorithmPanel algPanel = new AlgorithmPanel(this, algorithm);
				algPanel.addSolutionPanel(new ThroughputPanel(this, algorithm));
				algPanel.addSolutionPanel(new QueueLenPanel(this, algorithm));
				algPanel.addSolutionPanel(new ResTimePanel(this, algorithm));
				algPanel.addSolutionPanel(new SystemRespTimePanel(this, algorithm));
				algPanel.addSolutionPanel(new UtilizationPanel(this, algorithm));
				//Added by ASHANKA START
				// for System Power
				algPanel.addSolutionPanel(new SysPowerPanel(this, algorithm));
				//Added by ASHANKA STOP
				selector.addSolutionPanel(algPanel);
			}
			jtp.add(selector);
		} else if (selector != null && data.isClosed()) {
			SolverAlgorithm algorithm = data.getAlgorithmType();
			AlgorithmPanel algPanel = new AlgorithmPanel(this, algorithm);
			algPanel.addSolutionPanel(new ThroughputPanel(this, algorithm));
			algPanel.addSolutionPanel(new QueueLenPanel(this, algorithm));			
			algPanel.addSolutionPanel(new ResTimePanel(this, algorithm));
			algPanel.addSolutionPanel(new SystemRespTimePanel(this, algorithm));
			algPanel.addSolutionPanel(new UtilizationPanel(this, algorithm));
			//Added by ASHANKA START
			// for System Power
			algPanel.addSolutionPanel(new SysPowerPanel(this, algorithm));
			//Added by ASHANKA STOP
			selector.addSolutionPanel(algPanel);
			/*
			selector.addSolutionPanel(throughput);
			selector.addSolutionPanel(queuelength);
			selector.addSolutionPanel(restimes);
			selector.addSolutionPanel(utilizations);
			//Added by ASHANKA START
			// for System Power
			selector.addSolutionPanel(systemPower);
			*/
			//Added by ASHANKA STOP
			jtp.add(selector);
		} else if (selector != null) {
			SolverAlgorithm algorithm = data.getAlgorithmType();
			selector.addSolutionPanel(new ThroughputPanel(this, algorithm));
			selector.addSolutionPanel(new QueueLenPanel(this, algorithm));
			selector.addSolutionPanel(new ResTimePanel(this, algorithm));
			selector.addSolutionPanel(new SystemRespTimePanel(this, algorithm));
			selector.addSolutionPanel(new UtilizationPanel(this, algorithm));
			//Added by ASHANKA START
			// for System Power
			selector.addSolutionPanel(new SysPowerPanel(this, algorithm));
			//Added by ASHANKA STOP
			jtp.add(selector);
		} else {
			SolverAlgorithm alg = data.getAlgorithmType();
			ThroughputPanel throughput = new ThroughputPanel(this, alg);
			QueueLenPanel queuelength = new QueueLenPanel(this, alg);
			ResTimePanel restimes = new ResTimePanel(this, alg);
			SystemRespTimePanel systemreptime = new SystemRespTimePanel(this,alg);
			UtilizationPanel utilizations = new UtilizationPanel(this, alg);
			//Added by ASHANKA START
			//Introducing the new System Power Panel as a Tabbed Pane
			SysPowerPanel systemPower = new SysPowerPanel(this, alg);
			//Added by ASHANKA STOP
			
			jtp.add(throughput);
			jtp.add(queuelength);
			jtp.add(restimes);
			jtp.add(systemreptime);
			jtp.add(utilizations);
			//Added by ASHANKA START
			//for System Power
			jtp.add(systemPower);
			//Added by ASHANKA STOP
		}
		/* END */
		//NEW Dall'Orso 5-5-2005
		SynopsisPanel synPane;
		if (tempFile != null) {
			synPane = new SynopsisPanel(this, tempFile);
		} else {
			synPane = new SynopsisPanel(this);
		}
		jtp.add(synPane);
		//END
		//BoundingBox of main window
		Rectangle rect = this.getBounds();
		solutionWindow.setBounds(rect.x + 20, rect.y + 20, rect.width, rect.height);
		solutionWindow.setVisible(true);
	}

	//randomizes model data
	private void randomizeModel() {
		//TODO: CANCELLARE UNA VOLTA TROVATO IL BUG
		if (DEBUG) {
			System.out.println("Classes: " + data.getClasses() + "; Stations: " + data.getStations());
		}
		//first get infos about classes and station
		for (int i = 0; i < panels.size() && i < 2; i++) {
			Object o = panels.get(i);
			if (o instanceof ForceUpdatablePanel) {
				((ForceUpdatablePanel) o).commitData();
			}
		}
		//then randomize data
		data.randomizeModelData();
		//and then update all those data into panels
		ForceUpdatablePanel[] fuPanes = { (ForceUpdatablePanel) serviceDemandsPanel, (ForceUpdatablePanel) serviceTimesPanel,
				(ForceUpdatablePanel) serviceDemandsPanel };
		for (ForceUpdatablePanel fuPane : fuPanes) {
			fuPane.retrieveData();
		}
		repaint();
	}

	//END

	private void updatePanels() {
		if (data == null) {
			return;
		}

		for (int i = 0; i < panelCount; i++) {
			if (panels.get(i) instanceof WizardPanel) {
				(panels.get(i)).gotFocus();
			}
		}
		amvaPanel.update(null, null, null, null);
	}

	@Override
	protected void updateActions() {
		super.updateActions();
		if (currentIndex < (panelCount - 1)) {
			if (!tabbedPane.isEnabledAt(currentIndex + 1)) {
				ACTION_NEXT.setEnabled(false);
			}
		}
		if (currentIndex > 0 && currentIndex < tabbedPane.getComponentCount()) {
			if (!tabbedPane.isEnabledAt(currentIndex - 1)) {
				ACTION_PREV.setEnabled(false);
			}
		}
		updatePanels();
	}

	// JMVA MAIN
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.Plastic3DLookAndFeel());

		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		Locale.setDefault(Locale.ENGLISH);
		/* EDITED by Abhimanyu Chugh */
		new ExactWizard();
		//new ExactWizard(new ExactModel());
		/* END */
		
	}

	/**
	 * Sets the file name to be shown in the title
	 * @param filename the file name or null to remove it
	 */
	public void updateTile(String filename) {
		if (filename != null) {
			setTitle(TITLE + " - " + filename);
		} else {
			setTitle(TITLE);
		}
	}	
	
	/**
	 * Updates the algo panel
	 * @param isClosed true if model is closed, false if not, null to read from data structure
	 * @param isOpen true if model is open, false if not, null to read from data structure
	 * @param isAlgowhatif true if whatif on algorithm was selected, false if not, null to read from data structure
	 * @param isLoadDependent if model is load dependent or not. null to read from data structure
	 */
	public void updateAlgoPanel(Boolean isClosed, Boolean isOpen, Boolean isAlgowhatif, Boolean isLoadDependent) {
		this.amvaPanel.update(isClosed, isOpen, isAlgowhatif, isLoadDependent);
	}
}
