package nl.ctmm.trait.proteomics.qcviewer.input;

/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/

// TODO: check the license to make sure we can use this class. [Freek]

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;

/**
 * Class monitoring a {@link File} for changes.
 * 
 * @author Pascal Essiembre
 */
public class ProgressLogMonitor {
  /**
  * The logger for this class.
  */
  private static final Logger logger = Logger.getLogger(ProgressLogMonitor.class.getName());
  private static final ProgressLogMonitor instance = new ProgressLogMonitor();

  private Timer timer;

  private Hashtable<String, FileMonitorTask> timerEntries;

  /**
   * Gets the file monitor instance.
   * 
   * @return file monitor instance
   */
  public static ProgressLogMonitor getInstance() {
    return instance;
  }

  /**
   * Constructor.
   */
  private ProgressLogMonitor() {
	  prepareLogger(); 
    // Create timer, run timer thread as daemon.
    timer = new Timer(true);
    timerEntries = new Hashtable<>();
  }

	/**
   * Prepare the logger for this class
   * Set ConsoleHandler as handler
   * Set logging level to ALL 
   */
  private void prepareLogger() {
	  //Set logger and handler levels to Level.ALL
	  logger.setLevel(Level.ALL);
	  ConsoleHandler handler = new ConsoleHandler();
	  handler.setLevel(Level.ALL);
	  logger.addHandler(handler);
  }

/**
   * Adds a monitored file with a {@link FileChangeListener}.
   * 
   * @param listener
   *          listener to notify when the file changed.
   * @param fileName
   *          name of the file to monitor.
   * @param period
   *          polling period in milliseconds.
   */
  public void addFileChangeListener(FileChangeListener listener, String fileName, long period)
      throws FileNotFoundException {
    addFileChangeListener(listener, new File(FilenameUtils.normalize(fileName)), period);
  }

  /**
   * Adds a monitored file with a FileChangeListener.
   * 
   * @param listener
   *          listener to notify when the file changed.
   * @param file
   *          name of the file to monitor.
   * @param period
   *          polling period in milliseconds.
   */
  public void addFileChangeListener(FileChangeListener listener, File file, long period)
      throws FileNotFoundException {
    removeFileChangeListener(listener, file);
    FileMonitorTask task = new FileMonitorTask(listener, file);
    timerEntries.put(file.toString() + listener.hashCode(), task);
    timer.schedule(task, period, period);
  }

  /**
   * Remove the listener from the notification list.
   * 
   * @param listener
   *          the listener to be removed.
   */
  public void removeFileChangeListener(FileChangeListener listener, String fileName) {
    removeFileChangeListener(listener, new File(FilenameUtils.normalize(fileName)));
  }

  /**
   * Remove the listener from the notification list.
   * 
   * @param listener
   *          the listener to be removed.
   */
  public void removeFileChangeListener(FileChangeListener listener, File file) {
    FileMonitorTask task = timerEntries.remove(file.toString() + listener.hashCode());
    if (task != null) {
      task.cancel();
    }
  }

  /**
   * Fires notification that a file changed.
   * 
   * @param listener
   *          file change listener
   * @param file
   *          the file that changed
   */
  protected void fireFileChangeEvent(FileChangeListener listener, File file) {
    listener.fileChanged(file);
  }

  /**
   * File monitoring task.
   */
  class FileMonitorTask extends TimerTask {
    FileChangeListener listener;

    File monitoredFile;

    long lastModified;

    public FileMonitorTask(FileChangeListener listener, File file) throws FileNotFoundException {
      this.listener = listener;
      this.lastModified = 0;
      monitoredFile = file;
      if (!monitoredFile.exists()) { // but is it on CLASSPATH?
        URL fileURL = listener.getClass().getClassLoader().getResource(file.toString());
        if (fileURL != null) {
          monitoredFile = new File(fileURL.getFile());
        } else {
          throw new FileNotFoundException("File Not Found: " + file);
        }
      }
      this.lastModified = monitoredFile.lastModified();
    }

    public void run() {
      long lastModified = monitoredFile.lastModified();
      if (lastModified != this.lastModified) {
        this.lastModified = lastModified;
        fireFileChangeEvent(this.listener, monitoredFile);
      }
    }
  }
}

/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Pascal Essiembre - initial API and implementation
 ******************************************************************************/

/**
 * Listener interested in {@link File} changes.
 * 
 * @author Pascal Essiembre
 */
interface FileChangeListener {
  /**
   * Invoked when a file changes.
   * 
   * @param file
   *          name of changed file.
   */

  public void fileChanged(File file);
}