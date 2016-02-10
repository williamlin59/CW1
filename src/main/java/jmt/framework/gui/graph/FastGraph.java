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

package jmt.framework.gui.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.freehep.graphicsbase.util.export.ExportDialog;


/**
 * <p>Title: FastGraph</p>
 * <p>Description: Displays a graph with autoresizing property. This is designed to be a
 * really lightweight component as is supposed to be updated during simulation.
 * The component will draw data from a vector that can be changed to update the graph.
 * List must contain only object implementing </code>MeasureDefinition.Value</code> interface.
 * After updating List, user should call <code>repaint()</code> method to force update
 * of this graph. Labels on x axis are based on xunit value specified in the constructor or on SimulationTime.</p>
 * 
 * @author Bertoli Marco
 *         Date: 27-set-2005
 *         Time: 12.34.37
 */
public class FastGraph extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private static final Color COLOR_DRAW = Color.BLUE;
	private static final Color COLOR_AXIS = Color.BLACK;
	private static final Color COLOR_BG = Color.WHITE;
	private static final Color COLOR_BOUNDS = Color.RED;
	private static final Color COLOR_LAST_INTERVAL = Color.GREEN;
	private static final Color COLOR_POPUP = Color.decode("#C218FF");
	private static final Color COLOR_POPUP_BG = Color.decode("#FFFBF2");
	private static final int MARGIN = 8;
	
	private static final String POPUP_X_PREFIX = "t";
	private static final String POPUP_MIDDLE = " : ";
	private static final String POPUP_FULL_X_PREFIX = POPUP_X_PREFIX + POPUP_MIDDLE;
	private static final int POPUP_VSPACING = 1;
	private static final int POPUP_MARGIN = 5;
	
	private List<MeasureValue> values;
	private double xunit;
	private int x0; // Position of origin of cartesian axes

	private int y0;
	private double xstep; // Increment for each unit in pixels

	private double ystep;
	private boolean hideLastInterval;
	private MeasureValue selectedValue = null;
	protected PlotPopupMenu popup = new PlotPopupMenu();

	// Used to format numbers. Formatters are not thread safe, so they should never be static.
	private DecimalFormat decimalFormat0 = new DecimalFormat("0.0E0");
	private DecimalFormat decimalFormat1 = new DecimalFormat("#0.000");
	private DecimalFormat decimalFormat2 = new DecimalFormat("#0.00");
	private DecimalFormat decimalFormat3 = new DecimalFormat("#0.0");
	private DecimalFormat decimalFormat4 = new DecimalFormat("#00 ");

	private DecimalFormat formatXsimple  = new DecimalFormat("#0");
	private DecimalFormat formatXdecimal  = new DecimalFormat("#0.#");
	
	private DecimalFormat simulationTimeFormat = new DecimalFormat("#,##0");

	/**
	 * Builds a new FastGraph with specified input vector.
	 * @param values vector with values to be shown on graph (in MeasureValue format)
	 * @param xunit measure of unit of x axis. Each sample is distant from previous one of
	 * xunit.
	 */
	public FastGraph(List<MeasureValue> values, double xunit) {
		this.values = values;
		this.xunit = xunit;
	}

	/**
	 * Overrides default paint method to draw the graph
	 * @param g graphic component
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		int height = this.getHeight();
		int width = this.getWidth();

		// Draw graph area
		g.setColor(COLOR_BG);
		g.fillRect(MARGIN / 2, MARGIN / 2, width - MARGIN, height - MARGIN);

		// Aborts drawing if no elements are present
		if (values.size() < 1) {
			return;
		}

		// Aborts graph drawing if width is too small...
		if (width < 80) {
			return;
		}

		// Find maximum value for x
		MeasureValue lastValue = values.get(values.size() - 1);
		double lastXValue = getXValue(lastValue, values.size() - 1);
		
		// Detect X measure unit. It should be a multiple of 10^3
		String xLabel;
		double xMeasureUnit;
		double maxXLabel;
		boolean xDecimalDigit = false;
		{
			long xScale = (long)Math.log10(lastXValue);
			xScale = xScale - (xScale % 3);
			xMeasureUnit = Math.pow(10, xScale);
			
			// Allow one decimal digit if scaled value is less than 10
			maxXLabel = Math.ceil(lastXValue / xMeasureUnit);
			if (maxXLabel < 10) {
				maxXLabel = Math.ceil(lastXValue / xMeasureUnit * 10) / 10;
				xDecimalDigit = true;
			} 
			if (xScale > 1) {
				xLabel = "10^" + xScale;
			} else {
				xLabel = "";
			}
		}
		
		// Find maximum value for y
		double maxy = 0;
		for (int i = 0; i < values.size(); i++) {
			MeasureValue currValue = values.get(i);
			double currenty = currValue.getMeanValue();
			if (currenty > maxy && !Double.isInfinite(currenty)) {
				maxy = currenty;
			}
			currenty = currValue.getUpperBound();
			if (currenty > maxy && !Double.isInfinite(currenty)) {
				maxy = currenty;
			}
			if (!hideLastInterval) {
				currenty = currValue.getLastIntervalAvgValue();
				if (currenty > maxy && !Double.isInfinite(currenty)) {
					maxy = currenty;
				}
			}
		}
		// Correct zero maxy value, to avoid division per zero in ystep
		if (maxy == 0) {
			maxy = 1;
		}

		//Get text bounds
		FontMetrics metric = g.getFontMetrics();
		Rectangle2D xtextBound = metric.getStringBounds("XXXX", g);
		Rectangle2D ytextBound = metric.getStringBounds(formatNumber(maxy), g);
		Rectangle2D xLabelBound = metric.getStringBounds(xLabel, g);

		// Find initial position
		x0 = (int) Math.ceil(ytextBound.getWidth()) + 2 + MARGIN;
		y0 = height - (int) Math.ceil(xtextBound.getHeight()) - 12 - MARGIN;

		// Rounds the x axis step and adjust maxx accordingly.
		double maxAvailableXWidth = width - x0 - MARGIN - xtextBound.getWidth() / 2;
		int xTicNum = (int) Math.floor(maxAvailableXWidth / (xtextBound.getWidth() + 4));
		
		double xTicSize;
		if (!xDecimalDigit) {
			xTicSize = Math.ceil(maxXLabel / xTicNum);
		} else {
			xTicSize = Math.ceil(maxXLabel / xTicNum * 10) / 10;
		}
		double maxx = xTicSize * xTicNum * xMeasureUnit;
		
		xstep = (width - x0 - MARGIN - xtextBound.getWidth() / 2) / maxx;
		ystep = (y0 - MARGIN) / maxy;

		// Draws axis and captions
		g.setColor(COLOR_AXIS);
		// Y axis
		g.drawLine(x0, y0, x0, getY(maxy));
		int halfHeight = (int) Math.floor(ytextBound.getHeight() / 2);
		int num = (int) Math.floor((y0 - getY(maxy)) / (ytextBound.getHeight() + 2));
		// Draws caption for y axis
		for (int i = 0; i <= num; i++) {
			g.drawLine(x0, getY(maxy / num * i), x0 - 2, getY(maxy / num * i));
			g.drawString(formatNumber(maxy / num * i), MARGIN, getY(maxy / num * i) + halfHeight);
		}
		g.setColor(COLOR_AXIS);

		// X axis
		g.drawLine(x0, y0, getX(maxx), y0);
		
		
		num = (int) Math.floor((getX(maxx) - x0) / (xtextBound.getWidth() + 4));
		// Draws caption for x axis
		for (int i=0; i<=xTicNum;i++) {
			double axisValue = xTicSize * i;
			double unscaledAxisValue = axisValue * xMeasureUnit;
			String label;
			if (!xDecimalDigit) {
				label = formatXsimple.format(axisValue);
			} else {
				label = formatXdecimal.format(axisValue);
			}
			int halfWidth = (int) Math.floor(metric.getStringBounds(label,g).getWidth() / 2);
			g.drawLine(getX(unscaledAxisValue), y0, getX(unscaledAxisValue), y0 + 2);
			g.drawString(label, getX(unscaledAxisValue) - halfWidth, height - MARGIN - 12);
		}
		// Draws measure unit on X axis
		g.drawString(xLabel, width - (int)xLabelBound.getWidth() - MARGIN/2 - 1, height - MARGIN/2 - 1);

		// Draw chart series
		for (int i = 0; i < values.size() - 1; i++) {
			MeasureValue currValue = values.get(i);
			MeasureValue nextValue = values.get(i+1);
			double xValue = getXValue(currValue, i);
			double nextXValue = getXValue(nextValue, i+1);

			g.setColor(COLOR_BOUNDS);
			// upper bound
			if (currValue.getUpperBound() > 0 && !Double.isInfinite(currValue.getUpperBound())) {
				g.drawLine(getX(xValue),getY(currValue.getUpperBound()),getX(nextXValue),getY(nextValue.getUpperBound()));
			}

			// lower bound
			if (currValue.getLowerBound() > 0 && !Double.isInfinite(currValue.getLowerBound())) {
				g.drawLine(getX(xValue),getY(currValue.getLowerBound()),getX(nextXValue),getY(nextValue.getLowerBound()));
			}

			// average value
			g.setColor(COLOR_DRAW);
			g.drawLine(getX(xValue),getY(currValue.getMeanValue()),getX(nextXValue),getY(nextValue.getMeanValue()));

			// Draws last measured value
			if (hideLastInterval == false) {
				g.setColor(COLOR_LAST_INTERVAL);
				g.drawLine(getX(xValue),
						getY(0),
						getX((xValue)), getY(currValue.getLastIntervalAvgValue()));
			}
		}
		
		// Draw last points as dot if no lines were drawn during the previous cycle.
		if (values.size() == 1) {
			g.setColor(COLOR_BOUNDS);
			if (lastValue.getLowerBound() > 0 && !Double.isInfinite(lastValue.getLowerBound())) {
				g.fillOval(getX(lastXValue), getY(lastValue.getLowerBound()), 2, 1);
			}
			if (lastValue.getUpperBound() > 0 && !Double.isInfinite(lastValue.getUpperBound())) {
				g.fillOval(getX(lastXValue), getY(lastValue.getUpperBound()), 2, 1);
			}
			g.setColor(COLOR_DRAW);
			g.fillOval(getX(lastXValue),getY(lastValue.getMeanValue()), 2, 1);
		}
		
		if (hideLastInterval == false) {
			g.setColor(COLOR_LAST_INTERVAL);
			g.drawLine(getX(lastXValue),
					getY(0),
					getX((lastXValue)), getY(lastValue.getLastIntervalAvgValue()));
		}
		
		// Draws the selected value
		if (selectedValue != null) {
			paintSelectedValue(selectedValue, g, width, height, maxx, maxy);
		}
	}
	
	/**
	 * Draws the selected value
	 * @param selectedValue the selected value to draw
	 * @param g the graphics object
	 * @param width the total chart width
	 * @param height the total chart height
	 * @param maxx the maximum X value (simulation time)
	 * @param maxy the maximum Y value
	 */
	private void paintSelectedValue(MeasureValue selectedValue, Graphics g, int width, int height, double maxx, double maxy) {
		FontMetrics metric = g.getFontMetrics();
		String x_str = simulationTimeFormat.format(selectedValue.getSimTime());
		String m_str = formatNumber(selectedValue.getMeanValue());
		String i_str = formatNumber(selectedValue.getLastIntervalAvgValue());
		
		Dimension bounds = composeVerticalBounds(g, metric, POPUP_FULL_X_PREFIX + x_str, POPUP_FULL_X_PREFIX + m_str, POPUP_FULL_X_PREFIX + i_str);
		int selectedValueX = getX(selectedValue.getSimTime());
		int textX = (int)(selectedValueX - bounds.getWidth() / 2);
		// Fix value out of chart for label
		if (textX < 2) {
			textX = 2;
		} else if (textX + bounds.getWidth() + POPUP_MARGIN > width) {
			textX = width - (int)bounds.getWidth() - POPUP_MARGIN;
		}
		int textY = getY(maxy / 2) + (int)bounds.getHeight() / 2;
		
		g.setColor(COLOR_POPUP);
		g.drawLine(selectedValueX,
				getY(0),
				selectedValueX, getY(selectedValue.getLastIntervalAvgValue()));
		g.setColor(COLOR_POPUP_BG);
		g.fillRoundRect(textX - POPUP_MARGIN, textY - (int)bounds.getHeight(), (int)bounds.getWidth() + POPUP_MARGIN * 2, (int)bounds.getHeight() + POPUP_MARGIN, 4, 4);	
		g.setColor(COLOR_POPUP);
		g.drawRoundRect(textX - POPUP_MARGIN, textY - (int)bounds.getHeight(), (int)bounds.getWidth() + POPUP_MARGIN * 2, (int)bounds.getHeight() + POPUP_MARGIN, 4, 4);
		
		// Draw squares
		Rectangle2D prefixBounds = metric.getStringBounds(POPUP_X_PREFIX, g);
		g.setColor(COLOR_DRAW);
		g.fillRect(textX, textY - (int)prefixBounds.getHeight() - bounds.height / 3, (int)prefixBounds.getWidth(), (int)prefixBounds.getHeight());
		g.setColor(COLOR_LAST_INTERVAL);
		g.fillRect(textX, textY - (int)prefixBounds.getHeight(), (int)prefixBounds.getWidth(), (int)prefixBounds.getHeight());
		
		// Draws texts
		g.setColor(COLOR_AXIS);
		g.drawString(POPUP_FULL_X_PREFIX + x_str, textX, textY - bounds.height * 2 / 3);
		g.drawString(POPUP_MIDDLE + m_str, textX + (int)prefixBounds.getWidth(), textY - bounds.height / 3);
		g.drawString(POPUP_MIDDLE + i_str, textX + (int)prefixBounds.getWidth(), textY);
	}
	
	private Dimension composeVerticalBounds(Graphics g, FontMetrics metric, String...strings) {
		double xBounds = 0.0, yBounds = 0.0;
		for (int i=0; i<strings.length; i++) {
			Rectangle2D bounds = metric.getStringBounds(strings[i], g);
			if (xBounds < bounds.getWidth()) {
				xBounds = bounds.getWidth();
			}
			yBounds = yBounds + bounds.getHeight();
			if (i > 0) {
				yBounds = yBounds + POPUP_VSPACING;
			}
		}
		return new Dimension((int)xBounds, (int)yBounds);
	}

	/**
	 * Reads the X value for the chart
	 * @param value the measure value variable
	 * @param index the index
	 * @return the X value
	 */
	private double getXValue(MeasureValue value, int index) {
		if (value.getSimTime() > 0) {
			// For new models use simulation time
			return value.getSimTime();
		} else {
			// Compatibility with old models
			return xunit * index;
		}
	}



	/**
	 * Called when a right click is done on the chart
	 * @param ev the click event
	 */
	private void rightClick(MouseEvent ev) {
		popup.show(this, ev.getX(), ev.getY());
	}

	/**
	 * Called when a left click is done on the chart
	 * @param ev the click event
	 */
	private void leftClick(MouseEvent ev) {
		// If a value is already selected, removes selection.
		if (selectedValue != null) {
			selectedValue = null;
			repaint();
			return;
		}
		
		// Selects the best matching value.
		int clickedX = ev.getX();
		double simulationTime = (clickedX - x0) / xstep;
		int position = Collections.binarySearch(values, simulationTime, new SimTimeComparator());
		if (position < 0) {
			// Returns the best matching element. Checks position and the previous value.
			position = - position - 1;
			MeasureValue value = null;
			if (position > 0) {
				value = values.get(position - 1);
			}
			if (position < values.size()) {
				value = getNearestValue(value, values.get(position), simulationTime);
			}
			selectedValue = value;
		} else {
			// Exact match... We were extremely lucky.
			selectedValue = values.get(position);
		}
		repaint();
	}
	
	/**
	 * Returns the nearest value between left and right values
	 * @param leftValue one of the values
	 * @param rightValue the other values
	 * @param simulationTime the simulation time to match.
	 * @return the nearest value between the two.
	 */
	private MeasureValue getNearestValue(MeasureValue leftValue, MeasureValue rightValue, double simulationTime) {
		if (leftValue == null) {
			return rightValue;
		} else if (rightValue == null) {
			return leftValue;
		} else {
			double diff1 = Math.abs(leftValue.getSimTime() - simulationTime);
			double diff2 = Math.abs(rightValue.getSimTime() - simulationTime);
			if (diff1 < diff2) {
				return leftValue;
			} else {
				return rightValue;
			}
		}
	}

	/**
	 * Returns X coordinate for the screen of a point, given its value
	 * @param value value of point X
	 * @return X coordinate on the screen
	 */
	private int getX(double value) {
		return (int) Math.round(x0 + value * xstep);
	}

	/**
	 * Returns Y coordinate for the screen of a point, given its value
	 * @param value value of point Y
	 * @return Y coordinate on the screen
	 */
	private int getY(double value) {
		return (int) Math.round(y0 - value * ystep);
	}

	/**
	 * Formats a number to string to be shown as label of the graph
	 * @param value number to be converted
	 * @return value converted into string
	 */
	private String formatNumber(double value) {
		if (value == 0) {
			return "0.000";
		} else if (value < 0.001) {
			return decimalFormat0.format(value);
		} else if (value < 10) {
			return decimalFormat1.format(value);
		} else if (value < 100) {
			return decimalFormat2.format(value);
		} else if (value < 1000) {
			return decimalFormat3.format(value);
		} else {
			return decimalFormat4.format(value);
		}
	}

	/**
	 * Show/hide last interval display
	 * @param flag true to show last interval lines, false to hide them
	 */
	public void setHideLastInterval(boolean flag) {
		hideLastInterval = flag;
		repaint();
	}

	protected class PlotPopupMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;
		public JMenuItem saveAs;
		public PlotPopupMenu() {
			saveAs = new JMenuItem("Save as...");
			this.add(saveAs);
			addListeners();
		}
		public void addListeners() {
			saveAs.addActionListener(new AbstractAction() {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					ExportDialog export = new ExportDialog();
					export.showExportDialog(FastGraph.this,"Export view as ...", FastGraph.this,"Export");
				}});

		}

	}

	/**
	 * Invoked when mouse is clicked on the graph
	 * @param ev the click event
	 */
	public void mouseClicked(MouseEvent ev) {
		if (ev.getButton() == MouseEvent.BUTTON1) {
			leftClick(ev);
		} else if (ev.getButton() == MouseEvent.BUTTON3) {
			rightClick(ev);
		} else {
			repaint();
		}
	}

	/** Compares simulation time, either in a MeasureValue data structure or in a Double */
	private static class SimTimeComparator implements Comparator<Object> {
		@Override
		public int compare(Object arg0, Object arg1) {
			double val0 = getSimulationTime(arg0);
			double val1 = getSimulationTime(arg1);
			if (val0 < val1) {
				return -1;
			} else if (val0 > val1) {
				return 1;
			} else {
				return 0;
			}
		}
		
		/**
		 * Return the simulation time from the given object
		 * @param obj the object
		 * @return the simulation time
		 */
		private double getSimulationTime(Object obj) {
			if (obj instanceof Double) {
				return (Double) obj;
			} else if (obj instanceof MeasureValue) {
				return ((MeasureValue)obj).getSimTime();
			} else {
				return 0;
			}
		}
		
	}

}
