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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jmt.framework.data.ArrayUtils;
import jmt.framework.gui.graph.WhatIfPlot;
import jmt.framework.gui.table.editors.ColorCellEditor;
import jmt.framework.gui.table.editors.ComboBoxCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.SolverAlgorithm;

/**
 * <p>Title: Graph Panel</p>
 * <p>Description: This panel is used to display JMVA what-if analysis
 * results in a graph. Number of allowed lines in graph is determined
 * by <code>graph.getColors().length</code>. Modify it to allow more lines.</p>
 *
 * @author Bertoli Marco
 *         Date: 1-giu-2006
 *         Time: 11.01.29
 *         
 * @author Cerotti
 *  		Fixed results retrieval according to reference station
 * @version Date: JAN-2015
 */
public class GraphPanel extends WizardPanel implements ExactConstants {
	private static final long serialVersionUID = 1L;
	// Data structure
	private ExactModel model;
	// Plot
	private WhatIfPlot graph;
	// Performance index selector
	private JComboBox index;
	// Bounds for graph
	private JSpinner Xmin;
	private JSpinner Xmax;
	private JSpinner Ymin;
	private JSpinner Ymax;
	// Tells if spinner update is forced. This is needed to avoid that updates made by
	// code will be interpreted as updated made by user.
	private boolean forcedUpdate = false;
	// Table used to select performance indices to be plotted
	private LinesTable table;
	// Dimension of bounds spinners
	final static Dimension DIM_SPINNER = new Dimension(60, 20);
	// Current performance index
	private String currentIndex = "none";
	// Selected performance indices
	private int[] classes;
	private int[] stations;
	// Selected solver algorithm
	private SolverAlgorithm[] algorithms;
	private List<SolverAlgorithm> executedAlgorithms;
	// Aggregate special value
	private static final String AGGREGATE = "<html><b><i>Aggregate</i></b></html>";

	/**
	 * Builds a new GraphPanel, given an exact model data structure
	 * @param model reference to data structure
	 */
	public GraphPanel(ExactModel model) {
		this.model = model;
		initGraphics();
	}

	/**
	 * Initialize GUI of this panel
	 */
	private void initGraphics() {
		setLayout(new BorderLayout(10, 10));
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
		mainPanel.setBorder(BorderFactory.createEtchedBorder());

		// Adds description label
		JLabel descrLabel = new JLabel(DESCRIPTION_GRAPH);
		add(descrLabel, BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);

		// Creates left panel with options
		JPanel left = new JPanel(new BorderLayout(3, 3));
		// Adds performance index selection
		JPanel indexPanel = new JPanel();
		JLabel pIndex = new JLabel("Performance index: ");
		index = new JComboBox(ExactConstants.INDICES_TYPES);
		// Adds aggregate types
		for (String element : AGGREGATE_TYPES) {
			index.addItem(element);
		}
		pIndex.setLabelFor(index);
		indexPanel.add(pIndex);
		indexPanel.add(index);
		left.add(indexPanel, BorderLayout.NORTH);

		// Adds panel for bounds selection
		JPanel boundsPanel = new JPanel(new GridLayout(2, 4, 1, 1));
		boundsPanel.add(new JLabel("Xmin: ", SwingConstants.RIGHT));
		Xmin = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1e10, 0.01));
		Xmin.setPreferredSize(DIM_SPINNER);
		boundsPanel.add(Xmin);
		boundsPanel.add(new JLabel("Xmax: ", SwingConstants.RIGHT));
		Xmax = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1e10, 0.01));
		Xmax.setPreferredSize(DIM_SPINNER);
		boundsPanel.add(Xmax);
		boundsPanel.add(new JLabel("Ymin: ", SwingConstants.RIGHT));
		Ymin = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1e10, 0.01));
		Ymin.setPreferredSize(DIM_SPINNER);
		boundsPanel.add(Ymin);
		boundsPanel.add(new JLabel("Ymax: ", SwingConstants.RIGHT));
		Ymax = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1e10, 0.01));
		Ymax.setPreferredSize(DIM_SPINNER);
		boundsPanel.add(Ymax);
		left.add(boundsPanel, BorderLayout.SOUTH);

		mainPanel.add(left, BorderLayout.WEST);

		// Puts graph in the right panel
		// Creates label for X-axis
		String xLabel = "";
		if (model.getWhatIfClass() >= 0) {
			graph = new WhatIfPlot(model.getWhatIfValues());
			if (model.getWhatIfType().equals(ExactConstants.WHAT_IF_ARRIVAL)) {
				xLabel = "Arrival rate \u03bbi for " + model.getClassNames()[model.getWhatIfClass()] + " [job/s]";
			} else if (model.getWhatIfType().equals(ExactConstants.WHAT_IF_CUSTOMERS)) {
				xLabel = "Number of customers Ni for " + model.getClassNames()[model.getWhatIfClass()];
			} else if (model.getWhatIfType().equals(ExactConstants.WHAT_IF_DEMANDS)) {
				xLabel = "Service demand Di for " + model.getClassNames()[model.getWhatIfClass()] + " at "
						+ model.getStationNames()[model.getWhatIfStation()] + " [s]";
			} else if (model.getWhatIfType().equals(ExactConstants.WHAT_IF_MIX)) {
				xLabel = "Population mix \u03b2i for " + model.getClassNames()[model.getWhatIfClass()];
			}
		} else {
			graph = new WhatIfPlot(ArrayUtils.multiply(model.getWhatIfValues(), 100.0));
			if (model.getWhatIfType().equals(ExactConstants.WHAT_IF_ARRIVAL)) {
				xLabel = "% of arrival rates \u03bbi w.r.t. initial values";
			} else if (model.getWhatIfType().equals(ExactConstants.WHAT_IF_CUSTOMERS)) {
				xLabel = "% of customers Ni w.r.t. initial values";
			} else if (model.getWhatIfType().equals(ExactConstants.WHAT_IF_DEMANDS)) {
				xLabel = "% of Service demands Di at " + model.getStationNames()[model.getWhatIfStation()] + " w.r.t. initial values";
			}
		}
		graph.setXLabel(xLabel);
		mainPanel.add(graph, BorderLayout.CENTER);

		// Adds table and inits data structure for it.
		classes = new int[graph.getColors().length];
		if (model.isMultiClass()) {
			Arrays.fill(classes, -10);
		}
		stations = new int[graph.getColors().length];
		Arrays.fill(stations, -10);
		
		algorithms = new SolverAlgorithm[graph.getColors().length];
		Arrays.fill(algorithms, model.getAlgorithmType());
		if (model.isWhatifAlgorithms()) {
			executedAlgorithms = new ArrayList<SolverAlgorithm>(model.getWhatifAlgorithms());
		} else {
			executedAlgorithms = Arrays.asList(model.getAlgorithmType());
		}
		
		table = new LinesTable();
		JScrollPane tableScrollPane = new JScrollPane(table);
		tableScrollPane.setPreferredSize(new Dimension(160, tableScrollPane.getPreferredSize().height));
		left.add(tableScrollPane, BorderLayout.CENTER);
		graph.setLegendPanel(mainPanel);
		
		if (!model.isClosed()) {
			table.hideColumn(LinesTableColumn.ALGORITHM);
		}

		updateSpinners();
		addActions();
		updateIndex();
	}

	/**
	 * Updates values in spinners used to select ranges to be shown in graph
	 */
	private void updateSpinners() {
		// Check for special value used if graph is empty
		if (graph.getXRange()[0] != Double.MAX_VALUE) {
			Xmin.setValue(new Double(graph.getXRange()[0]));
			Xmax.setValue(new Double(graph.getXRange()[1]));
			Ymin.setValue(new Double(graph.getYRange()[0]));
			Ymax.setValue(new Double(graph.getYRange()[1]));
		} else {
			Xmin.setValue(new Double(0.0));
			Xmax.setValue(new Double(0.0));
			Ymin.setValue(new Double(0.0));
			Ymax.setValue(new Double(0.0));
		}
	}

	/**
	 * Used when a spinner value is updated
	 */
	private void setBounds() {
		double xmin, xmax, ymin, ymax;
		Object val = Xmin.getValue();
		if (val instanceof Number) {
			xmin = ((Number) val).doubleValue();
		} else {
			xmin = graph.getXRange()[0];
		}
		val = Xmax.getValue();
		if (val instanceof Number) {
			xmax = ((Number) val).doubleValue();
		} else {
			xmax = graph.getXRange()[1];
		}
		val = Ymin.getValue();
		if (val instanceof Number) {
			ymin = ((Number) val).doubleValue();
		} else {
			ymin = graph.getYRange()[0];
		}
		val = Ymax.getValue();
		if (val instanceof Number) {
			ymax = ((Number) val).doubleValue();
		} else {
			ymax = graph.getYRange()[1];
		}
		// Sets bounds
		graph.setXRange(xmin, xmax);
		graph.setYRange(ymin, ymax);
		graph.repaint();
	}

	/**
	 * This function must be called each time selected performance
	 * index changes
	 */
	private void updateIndex() {
		String current = (String) index.getSelectedItem();
		if (!current.equals(currentIndex)) {
			// Removes incorrect utilization measures
			if (table.getCellEditor() != null) {
				table.getCellEditor().stopCellEditing();
			}

			currentIndex = current;
			//Added by ASHANKA START
			if (currentIndex.equals(ExactConstants.INDICES_TYPES[4])) {
				//If the System Power is selected then Need to remove the Stations Column if present
				//If column count is less than 3 then do nothing as Stations Column is already removed.
				table.showColumn(LinesTableColumn.CLASS);
				table.hideColumn(LinesTableColumn.STATION);
			} else if (AGGREGATE_TYPES_SET.contains(currentIndex)) {
				table.hideColumn(LinesTableColumn.STATION);
				table.hideColumn(LinesTableColumn.CLASS);
			} else  {
				//If any thing other than System Power is clicked then 
				//restore the Stations Column only if it is not present.
				table.showColumn(LinesTableColumn.CLASS);
				table.showColumn(LinesTableColumn.STATION);
			}
			// This is an aggregated set
			if (AGGREGATE_TYPES_SET.contains(currentIndex)) {
				graph.clear(false);
				int i=0;
				for (SolverAlgorithm algo : executedAlgorithms) {
					// System response time
					if (currentIndex.equals(AGGREGATE_TYPES[0])) {
						graph.draw(i++, model.getGlobalR(algo, true));
					}
					// System throughput
					else if (currentIndex.equals(AGGREGATE_TYPES[1])) {
						graph.draw(i++, model.getGlobalX(algo));
					}
					// Number of customers
					else if (currentIndex.equals(AGGREGATE_TYPES[2])) {
						graph.draw(i++, model.getGlobalQ(algo));
					}
					//For single Class TableScroll Pane is removed. 
					else if (currentIndex.equals(ExactConstants.INDICES_TYPES[4]) && !model.isMultiClass()) {
						graph.draw(i++, model.getGlobalSP(algo, true));
					}
				}
				autosizeGraph();
			} else {
				if (currentIndex.equals(ExactConstants.INDICES_TYPES[3])) {
					for (int i = 0; i < stations.length; i++) {
						if (stations[i] == -1) {
							stations[i] = -2;
						}
					}
				}
				table.repaint();
				paintAllIndices();
			}
			
			// Updates graph
			graph.setYLabel(current);
			graph.repaint();
		}
	}

	/**
	 * Adds action listeners to GUI components
	 */
	private void addActions() {
		// Listener used for bounds spinners
		ChangeListener boundsListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!forcedUpdate) {
					setBounds();
					updateSpinners();
				}
			}
		};
		Xmin.addChangeListener(boundsListener);
		Xmax.addChangeListener(boundsListener);
		Ymin.addChangeListener(boundsListener);
		Ymax.addChangeListener(boundsListener);
		// Listener for index selection comboBox
		index.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateIndex();
			}
		});

		// Adds a listener to the graph to detect zoom events
		graph.addRescaleListener(new WhatIfPlot.RescaleListener() {
			public void Rescaled() {
				forcedUpdate = true;
				updateSpinners();
				forcedUpdate = false;
			}
		});
	}

	/**
	 * Paints performance index at specified row
	 * @param rowNum row number of index to be painted
	 */
	private void paintIndexAtRow(int rowNum) {
		// Clears previous graph
		graph.clear(rowNum);
		int classNum = classes[rowNum];
		int statNum = stations[rowNum];
		SolverAlgorithm alg = algorithms[rowNum];

		//Modified the below condition by ASHANKA for 
		//System Power there is no Station Panel
		//in fact the station panel is removed 
		//System Power is Indices type 4
		//if (classNum < -1 || statNum < -1) {
		if (classNum < -1 || (statNum < -1 && !currentIndex.equals(ExactConstants.INDICES_TYPES[4]))) {
			// Resets view
			autosizeGraph();
			return;
		}

		// Throughput
		if (currentIndex.equals(ExactConstants.INDICES_TYPES[0])) {
			if (classNum >= 0 && statNum >= 0) {
				graph.draw(rowNum, model.getThroughput(alg)[statNum][classNum]);
			} else if (classNum < 0 && statNum >= 0) {
				graph.draw(rowNum, model.getPerStationX(alg)[statNum]);
			} else if (classNum >= 0 && statNum < 0) {
				graph.draw(rowNum, model.getPerClassX(alg)[classNum]);
			} else {
				graph.draw(rowNum, model.getGlobalX(alg));
			}
		}
		// Queue length
		if (currentIndex.equals(ExactConstants.INDICES_TYPES[1])) {
			if (classNum >= 0 && statNum >= 0) {
				graph.draw(rowNum, model.getQueueLen(alg)[statNum][classNum]);
			} else if (classNum < 0 && statNum >= 0) {
				graph.draw(rowNum, model.getPerStationQ(alg)[statNum]);
			} else if (classNum >= 0 && statNum < 0) {
				graph.draw(rowNum, model.getPerClassQ(alg)[classNum]);
			} else {
				graph.draw(rowNum, model.getGlobalQ(alg));
			}
		}
		// Residence times
		if (currentIndex.equals(ExactConstants.INDICES_TYPES[2])) {
			if (classNum >= 0 && statNum >= 0) {
				graph.draw(rowNum, model.getResTimes(alg)[statNum][classNum]);
			} else if (classNum < 0 && statNum >= 0) {
				graph.draw(rowNum, model.getPerStationR(alg)[statNum]);
			} else if (classNum >= 0 && statNum < 0) {
				graph.draw(rowNum, model.getPerClassR(alg, true)[classNum]);
			} else {
				graph.draw(rowNum, model.getGlobalR(alg, true));
			}
		}
		// Utilization
		if (currentIndex.equals(ExactConstants.INDICES_TYPES[3])) {
			if (classNum >= 0 && statNum >= 0) {
				graph.draw(rowNum, model.getUtilization(alg)[statNum][classNum]);
			} else {
				graph.draw(rowNum, model.getPerStationU(alg)[statNum]);
			}
		}
		//Added by ASHANKA START
		//System Power
		else if (currentIndex.equals(ExactConstants.INDICES_TYPES[4])) {
			if (classNum >= 0) {
				graph.draw(rowNum, model.getPerClassSP(alg, true)[classNum]);
			} else if (classNum == -1) {
				graph.draw(rowNum, model.getGlobalSP(alg, true));
			}
		}
		/* END */
		
		//Added by ASHANKA STOP
		// Resets view
		autosizeGraph();
	}

	/**
	 * Paints all performance indices of current table
	 */
	private void paintAllIndices() {
		for (int i = 0; i < classes.length; i++) {
			paintIndexAtRow(i);
		}
	}

	/**
	 * AutoResizes graph window
	 */
	private void autosizeGraph() {
		graph.fillPlot();
	}

	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "Graphical Results";
	}

	private enum LinesTableColumn {
		COLOR(""), 
		CLASS("Class"), 
		STATION("Station"), 
		ALGORITHM("Algorithm"),
		HIDDEN("");
		
		private LinesTableColumn(String name) {
			this.name = name;
		}
		
		private String name;
		
		public String getName() {
			return name;
		}
	}

	/**
	 * Table used to select performance indices to be drawn
	 */
	protected class LinesTable extends JTable {
		private static final long serialVersionUID = 1L;
		/** ComboBoxes used as cell editors */
		private ComboEditor classEditor;
		/** ComboBoxes used as cell editors */
		private ComboEditor stationsEditor;
		/** ComboBoxes used as cell editors */
		private ComboEditor uStationsEditor;
		
		/** Edited by Georgios Poullaides **/
		private ComboAlgoEditor algorithmEditor;
		/** End **/

		/**
		 * Builds a new LinesTable
		 */
		public LinesTable() {
			super(new LinesTableModel());
			setDefaultRenderer(Color.class, new ColorCellEditor());
			setDefaultRenderer(String.class, ComboBoxCellEditor.getRendererInstance());
			setRowHeight(CommonConstants.ROW_HEIGHT);

			// Creates class editors (one is for utilizations)
			JComboBox classCombo = new JComboBox();
			// Null elements
			classCombo.addItem("");
			// Aggregate measures
			classCombo.addItem(AGGREGATE);
			for (int i = 0; i < model.getClasses(); i++) {
				classCombo.addItem(model.getClassNames()[i]);
			}

			// Creates station editor
			JComboBox stationsCombo = new JComboBox();
			JComboBox uStationsCombo = new JComboBox();
			stationsCombo.addItem("");
			uStationsCombo.addItem("");
			stationsCombo.addItem(AGGREGATE);
			uStationsCombo.addItem(ExactConstants.GRAY_S + AGGREGATE + ExactConstants.GRAY_E);
			for (int i = 0; i < model.getStations(); i++) {
				stationsCombo.addItem(model.getStationNames()[i]);
				uStationsCombo.addItem(model.getStationNames()[i]);
			}
			
			//Creates algorithm editor
			JComboBox algorithmCombo = new JComboBox();
			for (SolverAlgorithm algo : model.getWhatifAlgorithms()) {
				algorithmCombo.addItem(algo);
			}
			algorithmEditor = new ComboAlgoEditor(algorithmCombo);

			// Creates editors
			classEditor = new ComboEditor(classCombo);
			uStationsEditor = new ComboEditor(uStationsCombo);
			stationsEditor = new ComboEditor(stationsCombo);
		}
		
		

		@Override
		public void columnAdded(TableColumnModelEvent e) {
			LinesTableColumn type = getColumnType(e.getToIndex());
			TableColumn column = getColumnModel().getColumn(e.getToIndex());
			switch (type) {
				case COLOR:
					column.setMaxWidth(25);
					break;
				case CLASS:
					column.setPreferredWidth(90);
					break;
				case STATION:
					column.setPreferredWidth(90);
					break;
				case ALGORITHM:
					column.setPreferredWidth(100);
					break;
			}
			super.columnAdded(e);
		}



		/**
		 * Returns an appropriate editor for the cell specified by
		 * <code>row</code> and <code>column</code>. If the
		 * <code>TableColumn</code> for this column has a non-null editor,
		 * returns that.  If not, finds the class of the data in this
		 * column (using <code>getColumnClass</code>)
		 * and returns the default editor for this type of data.
		 * <p/>
		 *
		 * @param row    the row of the cell to edit, where 0 is the first row
		 * @param column the column of the cell to edit,
		 *               where 0 is the first column
		 * @return the editor for this cell;
		 *         if <code>null</code> return the default editor for
		 *         this type of cell
		 * @see javax.swing.DefaultCellEditor
		 */
		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			LinesTableColumn columnType = getColumnType(column);
			switch (columnType) {
				case CLASS:
					return classEditor;
				case STATION:
					if (currentIndex.equals(ExactConstants.INDICES_TYPES[3])) {
						return uStationsEditor;
					} else {
						return stationsEditor;
					}
				case ALGORITHM:
					return algorithmEditor;
			}
			return null;
		}

		/**
		 * Returns an appropriate renderer for the cell specified by this row and
		 * column. If the <code>TableColumn</code> for this column has a non-null
		 * renderer, returns that.  If not, finds the class of the data in
		 * this column (using <code>getColumnClass</code>)
		 * and returns the default renderer for this type of data.
		 * <p/>
		 * <b>Note:</b>
		 * Throughout the table package, the internal implementations always
		 * use this method to provide renderers so that this default behavior
		 * can be safely overridden by a subclass.
		 *
		 * @param row    the row of the cell to render, where 0 is the first row
		 * @param column the column of the cell to render,
		 *               where 0 is the first column
		 * @return the assigned renderer; if <code>null</code>
		 *         returns the default renderer
		 *         for this type of object
		 * @see javax.swing.table.DefaultTableCellRenderer
		 * @see javax.swing.table.TableColumn#setCellRenderer
		 * @see #setDefaultRenderer
		 */
		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (getModel().isCellEditable(row, column) || getColumnType(column) == LinesTableColumn.COLOR) {
				return super.getCellRenderer(row, column);
			} else {
				return super.getDefaultRenderer(Object.class);
			}
		}
		
		

		/* (non-Javadoc)
		 * @see javax.swing.JTable#getModel()
		 */
		@Override
		public LinesTableModel getModel() {
			return (LinesTableModel) super.getModel();
		}
		
		/**
		 * Return the type of column
		 * @param columnIndex the column index
		 * @return the type of column
		 */
		protected LinesTableColumn getColumnType(int columnIndex) {
			return getModel().getColumn(columnIndex);
		}
		
		/**
		 * Shows the given column
		 * @param column the column to show
		 */
		public void showColumn(LinesTableColumn column) {
			getModel().showColumn(column);
		}
		
		/**
		 * Hides the given column
		 * @param column the column to hide
		 */
		public void hideColumn(LinesTableColumn column) {
			getModel().hideColumn(column);
		}

		/**
		 * Inner class used as a comboBox editor
		 */
		protected class ComboEditor extends DefaultCellEditor {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			protected JComboBox combo;

			/**
			 * Constructs a <code>DefaultCellEditor</code> object that uses a
			 * combo box.
			 *
			 * @param comboBox a <code>JComboBox</code> object
			 */
			public ComboEditor(JComboBox comboBox) {
				super(comboBox);
				combo = comboBox;
			}

			/**
			 * Returns selected index - 2 (so -1 means all classes and -2 or -3
			 * means no selection)
			 */
			@Override
			public Object getCellEditorValue() {
				int val = combo.getSelectedIndex();
				return new Integer(val - 2);
			}
		}

		/**
		 * Inner class used as a comboBox editor
		 */
		protected class ComboAlgoEditor extends ComboEditor {
			private static final long serialVersionUID = 1L;

			/**
			 * Constructs a <code>DefaultCellEditor</code> object that uses a
			 * combo box.
			 *
			 * @param comboBox a <code>JComboBox</code> object
			 */
			public ComboAlgoEditor(JComboBox comboBox) {
				super(comboBox);
			}

			/**
			 * Returns selected algorithm
			 */
			@Override
			public Object getCellEditorValue() {
				return combo.getSelectedItem();
			}
		}
}

	/**
	 * Table model for LinesTable
	 */
	private class LinesTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		
		private Set<LinesTableColumn> columns;
		private List<LinesTableColumn> columnsList;
		
		public LinesTableModel() {
			columns = EnumSet.allOf(LinesTableColumn.class);
			columns.remove(LinesTableColumn.HIDDEN);
			columnsList = new ArrayList<GraphPanel.LinesTableColumn>(columns);
		}
		
		public LinesTableColumn getColumn(int index) {
			if (columnsList.size() > index) {
				return columnsList.get(index);
			} else {
				return LinesTableColumn.HIDDEN;
			}
		}
		
		/**
		 * Shows the given column
		 * @param column the column to show
		 */
		public void showColumn(LinesTableColumn column) {
			columns.add(column);
			columnsList = new ArrayList<GraphPanel.LinesTableColumn>(columns);
			super.fireTableStructureChanged();
		}
		
		/**
		 * Hides the given column
		 * @param column the column to hide
		 */
		public void hideColumn(LinesTableColumn column) {
			columns.remove(column);
			columnsList = new ArrayList<GraphPanel.LinesTableColumn>(columns);
			super.fireTableStructureChanged();
		}

		/**
		 * Returns <code>Object.class</code> regardless of <code>columnIndex</code>.
		 *
		 * @param columnIndex the column being queried
		 * @return the Object.class
		 */
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return getColumn(columnIndex) == LinesTableColumn.COLOR ? Color.class : String.class;
		}

		/**
		 * Returns the number of columns in the model. A
		 * <code>JTable</code> uses this method to determine how many columns it
		 * should create and display by default.
		 *
		 * @return the number of columns in the model
		 * @see #getRowCount
		 */
		public int getColumnCount() {
			return columnsList.size();
		}

		/**
		 * Returns a default name for the column using spreadsheet conventions:
		 * A, B, C, ... Z, AA, AB, etc.  If <code>column</code> cannot be found,
		 * returns an empty string.
		 *
		 * @param column the column being queried
		 * @return a string containing the default name of <code>column</code>
		 */
		@Override
		public String getColumnName(int column) {
			return getColumn(column).getName();
		}

		/**
		 * Returns the number of rows in the model. A
		 * <code>JTable</code> uses this method to determine how many rows it
		 * should display.  This method should be quick, as it
		 * is called frequently during rendering.
		 *
		 * @return the number of rows in the model
		 * @see #getColumnCount
		 */
		public int getRowCount() {
			return graph.getColors().length;
		}

		/**
		 * Returns the value for the cell at <code>columnIndex</code> and
		 * <code>rowIndex</code>.
		 *
		 * @param    rowIndex    the row whose value is to be queried
		 * @param    columnIndex the column whose value is to be queried
		 * @return the value Object at the specified cell
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			int stationNum = stations[rowIndex];
			int classNum = classes[rowIndex];
			int algoNum = 1;
			if (model.isWhatifAlgorithms()) {
				algoNum = model.getWhatifAlgorithms().size();
			}
			
			LinesTableColumn column = getColumn(columnIndex);
			boolean aggregate = AGGREGATE_TYPES_SET.contains(currentIndex);
			
			switch (column) {
				case COLOR:
					return graph.getColors()[rowIndex];
				case CLASS:
					if (aggregate) {
						if (rowIndex < algoNum) {
							return AGGREGATE;
						} else {
							return null;
						}
					} else if (classNum >= 0) {
						return model.getClassNames()[classNum];
					} else if (classNum == -1) {
						return AGGREGATE;
					} else {
						return null;
					}
				case STATION:
					if (aggregate) {
						if (rowIndex < algoNum) {
							return AGGREGATE;
						} else {
							return null;
						}
					} else if (stationNum >= 0) {
						return model.getStationNames()[stationNum];
					} else if (stationNum == -1) {
						return AGGREGATE;
					} else {
						return null;
					}
				case ALGORITHM:
					if (aggregate) {
						if (rowIndex < algoNum) {
							return executedAlgorithms.get(rowIndex);
						} else {
							return null;
						}
					} else {
						return algorithms[rowIndex];
					}
			}
			return null;
		}
		
		/**
		 * This empty implementation is provided so users do not have to implement
		 * this method if their data model is not editable.
		 *
		 * @param aValue      value to assign to cell
		 * @param rowIndex    row of cell
		 * @param columnIndex column of cell
		 */
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			LinesTableColumn column = getColumn(columnIndex);
			
			switch (column) {
			case CLASS:
				classes[rowIndex] = ((Integer) aValue).intValue();
				break;
			case STATION:
				if (currentIndex.equals(ExactConstants.INDICES_TYPES[3]) && ((Integer) aValue).intValue() < 0) {
					stations[rowIndex] = -2;
				} else {
					stations[rowIndex] = ((Integer) aValue).intValue();
				}
				break;
			case ALGORITHM:
				algorithms[rowIndex] = (SolverAlgorithm)aValue;
				break;
			
			}
			
			// Paints new index
			paintIndexAtRow(rowIndex);
		}

		/**
		 * Class and stations are editables
		 *
		 * @param rowIndex    the row being queried
		 * @param columnIndex the column being queried
		 * @return false
		 */
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			// Nothing is editable for aggregate indices
			if (AGGREGATE_TYPES_SET.contains(currentIndex)) {
				return false;
			}
			LinesTableColumn column = getColumn(columnIndex);
			
			switch (column) {
				case CLASS:
					return model.isMultiClass();
				case STATION:
					return true;
				case ALGORITHM:
					return model.isWhatifAlgorithms();
			}
			return false;
		}
	}
}
