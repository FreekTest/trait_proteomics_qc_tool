package nl.ctmm.trait.proteomics.qcviewer.gui;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JDesktopPane;
import javax.swing.Scrollable;

public class ScrollDesktop extends JDesktopPane implements Scrollable {
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  public int getScrollableUnitIncrement(Rectangle r, int axis, int dir) {
    return 50;
  }

  public int getScrollableBlockIncrement(Rectangle r, int axis, int dir) {
    return 200;
  }

  public boolean getScrollableTracksViewportWidth() {
    return false;
  }

  public boolean getScrollableTracksViewportHeight() {
    return false;
  }
}


