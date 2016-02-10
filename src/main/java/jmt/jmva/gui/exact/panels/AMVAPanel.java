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

package jmt.jmva.gui.exact.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jmt.framework.gui.help.HoverHelp;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.SolverAlgorithm;
import jmt.jmva.analytical.solvers.SolverMultiClosedAMVA;
import jmt.jmva.analytical.solvers.SolverMultiClosedMonteCarlo;
import jmt.jmva.gui.exact.ExactWizard;

/**
 * Panel representing the combo box on ExactWizard (GUI of JMVA)
 * 
 * @author Abhimanyu Chugh, Marco Bertoli
 *
 */
public final class AMVAPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String LABEL_ALGORITHM = "Algorithm:";
	private static final String LABEL_ALGORITHM_MIXED = LABEL_ALGORITHM + " MVA";
	private static final String LABEL_ALGORITHM_OPEN = LABEL_ALGORITHM + " QN";
	private static final String LABEL_ALGORITHM_WHATIF = LABEL_ALGORITHM + " (what-if)";

    private static final String MONTE_CARLO = "Monte Carlo";
	
	private static enum PanelStatus {
		ENABLED(LABEL_ALGORITHM, true, false, false),
		ENABLED_TOL(LABEL_ALGORITHM, true, true, false),
        ENABLED_MAX_SAMPLE(LABEL_ALGORITHM, true, true, true),
		DISABLED_OPEN(LABEL_ALGORITHM_OPEN, false, false, false),
		DISABLED_MIXED(LABEL_ALGORITHM_MIXED, false, false, false),
		DISABLED_WHATIF(LABEL_ALGORITHM_WHATIF, false, false, false);
		
		private String algorithmLabel;
		private boolean tolleranceVisible;
		private boolean selectorVisible;
        private boolean maxSamplesVisible;
		
		private PanelStatus(String algorithmLabel, boolean selectorVisible, boolean tolleranceVisible, boolean maxSamplesVisible) {
			this.algorithmLabel = algorithmLabel;
			this.selectorVisible = selectorVisible;
			this.tolleranceVisible = tolleranceVisible;
            this.maxSamplesVisible = maxSamplesVisible;
		}
	
		
	};
	
	private HoverHelp help;
	private NumberFormat numFormat = new DecimalFormat("#.###############");

	private ExactWizard ew;
	
	private JLabel tolLabel;
	private JLabel algLabel;
	private JTextField tolerance;
    private JLabel maxSampleLabel;
    private JTextField maxSamples;
	private JComboBox algorithmList;
	private String [] modelName;
	private PanelStatus status = PanelStatus.ENABLED;
	
	
	private ActionListener ACTION_CHANGE_ALGORITHM = new ActionListener() {
		// initial value
		int currentItem = 1;
		
		public void actionPerformed(ActionEvent e) {
			JComboBox algorithmList = (JComboBox)e.getSource();
			String algorithm = (String)algorithmList.getSelectedItem();
			
			// check if algorithm or not
			SolverAlgorithm algo = SolverAlgorithm.fromString(algorithm);
			if (algo == null) {
				algorithmList.setSelectedIndex(currentItem);
			} else {
				currentItem = algorithmList.getSelectedIndex();
				ew.getData().setAlgorithmType(algo);
				if (algo.isExact()) {
                    updateStatus(PanelStatus.ENABLED);
				} else {
                    if (algorithm.equals(MONTE_CARLO)) {
                        updateStatus(PanelStatus.ENABLED_MAX_SAMPLE);
                    } else {
                        updateStatus(PanelStatus.ENABLED_TOL);
                    }
				}
			}
		}
	};

    private MaxSampleInputListener ACTION_CHANGE_MAX_SAMPLE = new MaxSampleInputListener();

	private ToleranceInputListener ACTION_CHANGE_TOLERANCE = new ToleranceInputListener();

	public AMVAPanel(ExactWizard ew) {
		super(new BorderLayout());
		this.ew = ew;
		help = ew.getHelp();

		int index = 0;
		String[] names = SolverAlgorithm.closedNames();
		modelName = new String[names.length+2];
		int noOfExactAlgs = SolverAlgorithm.noOfExactAlgs();
		for (int i = 0; i < names.length; i++) {
			if (i == 0) {
				modelName[index] = "--------- Exact ---------";
				index++;
			} else if (i == noOfExactAlgs) {
				modelName[index] = "----- Approximate -----";
				index++;
			}
			modelName[index] = names[i];
			index++;
		}

		initialize();
	}

	/**
	 * Initialize this panel
	 */
	private void initialize() {
		JPanel mainPanel = new JPanel(new FlowLayout());
		mainPanel.add(algLabel());
		mainPanel.add(algorithmList());
		mainPanel.add(tolLabel());
		mainPanel.add(tolerance());
        mainPanel.add(maxSampleLabel());
        mainPanel.add(maxSamples());
		this.add(mainPanel, BorderLayout.WEST);
	}

	private JComponent algorithmList() {
		Dimension d = new Dimension(160,30);
		algorithmList = new JComboBox(modelName);
		algorithmList.setMaximumSize(d);
		algorithmList.setSelectedIndex(1);
		algorithmList.addActionListener(ACTION_CHANGE_ALGORITHM);
		algorithmList.setVisible(status.selectorVisible);
		algorithmList.setRenderer(new DefaultListCellRenderer() {
            /**
             *
             */
            private static final long serialVersionUID = -6074991824391523738L;

            @Override
            public Component getListCellRendererComponent(JList list,
                                                          Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                String str = (value == null) ? "" : value.toString();
                if (SolverAlgorithm.fromString(str) == null) {
                    comp.setEnabled(false);
                    comp.setFocusable(false);
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                } else {
                    comp.setEnabled(true);
                    comp.setFocusable(true);
                }
                return comp;
            }
        });
		help.addHelp(algorithmList, "Algorithm for solving model");
		return algorithmList;
	}

	private JComponent tolLabel() {
		Dimension d = new Dimension(70,30);
		tolLabel = new JLabel("  Tolerance:");
		tolLabel.setMaximumSize(d);
		tolLabel.setFocusable(false);
		tolLabel.setVisible(status.tolleranceVisible);
		return tolLabel;
	}

	private JComponent tolerance() {
		Dimension d = new Dimension(80,30);
		tolerance = new JTextField(10);
		tolerance.setText(numFormat.format(ew.getData().getTolerance()));
		tolerance.setMaximumSize(d);
		help.addHelp(tolerance, "Input Tolerance for AMVA Algorithms");
		tolerance.setFocusable(true);
		tolerance.addKeyListener(ACTION_CHANGE_TOLERANCE);
		tolerance.addFocusListener(ACTION_CHANGE_TOLERANCE);
		tolerance.setVisible(status.tolleranceVisible);
		return tolerance;
	}

    private JComponent maxSampleLabel() {
        Dimension d = new Dimension(70,30);
        maxSampleLabel = new JLabel("  Max number of samples:");
        maxSampleLabel.setMaximumSize(d);
        maxSampleLabel.setFocusable(false);
        maxSampleLabel.setVisible(status.maxSamplesVisible);
        return maxSampleLabel;
    }

	private JComponent maxSamples() {
        Dimension d = new Dimension(80,30);
        maxSamples = new JTextField(10);
        maxSamples.setText(numFormat.format(ew.getData().getMaxSamples()));
        maxSamples.setMaximumSize(d);
        help.addHelp(maxSamples, "Max samples needed for Monte Carlo Algorithm");
        maxSamples.setFocusable(true);
        maxSamples.addKeyListener(ACTION_CHANGE_MAX_SAMPLE);
        maxSamples.addFocusListener(ACTION_CHANGE_MAX_SAMPLE);
        maxSamples.setVisible(status.maxSamplesVisible);
        return maxSamples;
    }

	/**
	 * Updates the algo panel
	 * @param isClosed true if model is closed, false if not, null to read from data structure
	 * @param isOpen true if model is open, false if not, null to read from data structure
	 * @param isAlgowhatif true if whatif on algorithm was selected, false if not, null to read from data structure
	 * @param isLoadDependent if model is load dependent or not. null to read from data structure
	 */
	public void update(Boolean isClosed, Boolean isOpen, Boolean isAlgowhatif, Boolean isLoadDependent) {
		ExactModel data = ew.getData();
		SolverAlgorithm algorithm = data.getAlgorithmType();
		tolerance.setText(numFormat.format(data.getTolerance()));
        maxSamples.setText(numFormat.format(data.getMaxSamples()));
		algorithmList.setSelectedItem(algorithm.toString());
		if (isClosed == null) {
			isClosed = ew.getData().isClosed();
		}
		if (isOpen == null) {
			isOpen = ew.getData().isOpen();
		}
		if (isAlgowhatif == null) {
			isAlgowhatif = ew.getData().isWhatifAlgorithms();
		}
		if (isLoadDependent == null) {
			isLoadDependent = ew.getData().isLd();
		}

		if (isLoadDependent) {
			updateStatus(PanelStatus.DISABLED_MIXED);
		} else if (isAlgowhatif) {
			updateStatus(PanelStatus.DISABLED_WHATIF);
		} else if (isOpen) {
			updateStatus(PanelStatus.DISABLED_OPEN);
		} else if (!isClosed) {
			updateStatus(PanelStatus.DISABLED_MIXED);
		} else if (!algorithm.isExact()) {
            if (algorithm.toString().equals(MONTE_CARLO)) {
                updateStatus(PanelStatus.ENABLED_MAX_SAMPLE);
            } else {
                updateStatus(PanelStatus.ENABLED_TOL);
            }
		} else {
			updateStatus(PanelStatus.ENABLED);
		}
	}

	/**
	 * Updates the panel status
	 * @param newStatus the new panel status
	 */
	private void updateStatus(PanelStatus newStatus) {
		if (status == newStatus) {
			return;
		}
		algLabel.setText(newStatus.algorithmLabel);
        algLabel.setEnabled(newStatus != PanelStatus.DISABLED_WHATIF);
        algorithmList.setVisible(newStatus.selectorVisible);
		tolLabel.setVisible(newStatus.tolleranceVisible);
		tolerance.setVisible(newStatus.tolleranceVisible);
        maxSampleLabel.setVisible(newStatus.maxSamplesVisible);
        maxSamples.setVisible(newStatus.maxSamplesVisible);
		status = newStatus;
	}

	private JComponent algLabel() {
		Dimension d = new Dimension(65,30);
		algLabel = new JLabel(status.algorithmLabel);
		algLabel.setMaximumSize(d);
		algLabel.setFocusable(false);
		help.addHelp(algLabel, "Algorithm used to solve the model");
		return algLabel;
	}

	private void updateTolerance() {
		Double tol = SolverMultiClosedAMVA.validateTolerance(tolerance.getText());
		if (tol != null) {
			ew.getData().setTolerance(tol);
		}
		else {
			JOptionPane.showMessageDialog(ew, "Error: Invalid tolerance value. Using last valid value.", "Input data error", JOptionPane.ERROR_MESSAGE);
		}
	}

    private void updateMaxSamples() {
        ExactModel model = ew.getData();
        Integer mSamples = SolverMultiClosedMonteCarlo.validateMaxSamples(maxSamples.getText());
        if (mSamples != null) {
            model.setMaxSamples(mSamples);
        } else {
            JOptionPane.showMessageDialog(ew, "Error: Invalid max samples value. Using last valid value.", "Input data error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class MaxSampleInputListener implements KeyListener, FocusListener {
        @Override
        public void focusLost(FocusEvent e) {
            updateMaxSamples();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                updateMaxSamples();
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }


        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

	private class ToleranceInputListener implements KeyListener, FocusListener {
		@Override
		public void focusLost(FocusEvent e) {
			updateTolerance();
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				updateTolerance();
			}
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}


		@Override
		public void keyReleased(KeyEvent e) {
		}
	}
}
