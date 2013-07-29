package nl.ctmm.trait.proteomics.qcviewer.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import nl.ctmm.trait.proteomics.qcviewer.utils.Constants;

import org.apache.commons.io.FilenameUtils;
import org.jfree.data.xy.XYSeries;

/**
 * This class contains the logic to read the directory/file structure and prepare data to be displayed.
 *
 * @author <a href="mailto:pravin.pawar@nbic.nl">Pravin Pawar</a>
 * @author <a href="mailto:freek.de.bruijn@nbic.nl">Freek de Bruijn</a>
 */
public class ReportReader extends JFrame {
    /**
     * The logger for this class.
     */
    private static final Logger logger = Logger.getLogger(ReportReader.class.getName());

    /**
     * The version number for (de)serialization of this class (UID: universal identifier).
     */
    private static final long serialVersionUID = 1;

    private static final List<String> MONTH_DIRS = Arrays.asList(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    );

    private int currentReportNum;
    private JsonMetricsReader jsonMetricsReader;
    
    public ReportReader(final MetricsParser metricsParser) {
        jsonMetricsReader = new JsonMetricsReader(metricsParser);
    }

	/**
     * Search through the directories under the root directory for files generated by the QC tool and return the
     * relevant data.
     *
     * @param rootDirectoryName the root directory that contains the year directories.
     * @param fromDate the start of the date range to search.
     * @param tillDate the end of the date range to search.
     * @return a list with report units.
     */
    public ArrayList<ReportUnit> retrieveReports(final String rootDirectoryName, final Date fromDate, final Date tillDate) {
        /*The directory has three levels - year, month and msrun.
        The msrun directory may contain following three files of importance:
        1) metrics.json: String file containing values of all QC metrics in json object format 
        2) msrun*_ticmatrix.csv
        */
//        String allErrorMessages = "";
        final ArrayList<ReportUnit> reportUnits = new ArrayList<>();
        logger.log(Level.ALL, "Root folder = " + rootDirectoryName);
        for (final File yearDirectory : getYearDirectories(FilenameUtils.normalize(rootDirectoryName))) {
            logger.fine("Year = " + yearDirectory.getName());
            for (final File monthDirectory : getMonthDirectories(yearDirectory)) {
                logger.fine("Month = " + monthDirectory.getName());
                for (final File msRunDirectory : getMsRunDirectories(monthDirectory)) {
                    logger.fine("Msrun = " + msRunDirectory.getName());
                    long datetime = msRunDirectory.lastModified();
                    Date d = new Date(datetime);
                    SimpleDateFormat sdf = new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT_STRING);
                    String dateString = sdf.format(d);
                    try {
                        d = sdf.parse(dateString);
                    } catch (ParseException e) {
                    	logger.log(Level.SEVERE, "Something went wrong while parsing dates", e);
                    }
                    if (d.compareTo(fromDate)>=0 && d.compareTo(tillDate)<=0) {
                        final File[] dataFiles = msRunDirectory.listFiles();
                        boolean errorFlag = false;
                        //Check existence of "metrics.json", "_ticmatrix.csv"
                        String errorMessage = checkDataFilesAvailability(msRunDirectory.getName(), dataFiles);
                        if (!errorMessage.equals("")) {
                            errorFlag = true;
                            //allErrorMessages += errorMessage + "\n";
                        }
                        reportUnits.add(createReportUnit(msRunDirectory.getName(), dataFiles, errorFlag));
                    } 
                }
            }
        }
        /*TODO: Check whether errorMessages functionality is required
         * if (!allErrorMessages.equals("")) {
            //saveErrorMessages(allErrorMessages);
            //JOptionPane.showMessageDialog(this, allErrorMessages, "MSQC Check Warning Messages",
            //                              JOptionPane.ERROR_MESSAGE);
        }*/
        return reportUnits;
    }

    /**
     * Check whether the report directory contains "metrics.json", and "_ticmatrix.csv" files
     * @param msrunName Folder containing QC Report
     * @param dataFiles list of files in folder msrunName
     * @return errorMessage if the "metrics.json", and "_ticmatrix.csv" files not found
     */
    private String checkDataFilesAvailability(final String msrunName, final File[] dataFiles) {
        String errorMessage = "";
        boolean metrics = false, ticMatrix = false;
        for (final File dataFile : dataFiles) {
            final String dataFileName = dataFile.getName();
            if (dataFile.isFile()) {
                logger.fine("File " + dataFileName);
                if (dataFileName.equals("metrics.json")) {
                       metrics = true;
                } else if (dataFileName.endsWith("_ticmatrix.csv")) {
                    ticMatrix = true;
                }
            }
        }
        if (!metrics || !ticMatrix) {
            errorMessage = "<html>In Folder " + msrunName + " following file types are missing:";
            if (!metrics) {
                errorMessage += "metrics.json ";
            }
            if (!ticMatrix) {
                errorMessage += "_ticmatrix.csv ";
            }
            errorMessage += "</html>";
        }
        return errorMessage;
    }
    
//    /**
//     * Save errorMessages to errorMessages.txt file
//     * @param allErrorMessages
//     */
//    private void saveErrorMessages(String allErrorMessages) {
//        try {
//            //Save errorMessages to errorMessages.txt file
//            FileWriter fWriter = new FileWriter(FilenameUtils.normalize("QCReports\\errorMessages.txt"), true);
//            BufferedWriter bWriter = new BufferedWriter(fWriter);
//            Date date = new Date();
//            bWriter.write(date.toString() + "\n");
//            bWriter.write(allErrorMessages + "\n");
//            bWriter.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * Retrieve the year directories in the root directory.
     *
     * @param rootDirectoryName the name of the root directory in which to search for year directories.
     * @return the list of year directories.
     */
    private List<File> getYearDirectories(final String rootDirectoryName) {
        final List<File> yearDirectories = new ArrayList<>();
        final File rootDirectory = new File(FilenameUtils.normalize(rootDirectoryName));
        if (rootDirectory.exists()) {
            final File[] yearFiles = rootDirectory.listFiles();
            if (yearFiles != null) {
                for (final File yearFile : yearFiles) {
                    if (isYearDirectory(yearFile)) {
                        yearDirectories.add(yearFile);
                    }
                }
            }
        }
        return yearDirectories;
    }

    /**
     * Check whether a year file object is a directory with a four digit name.
     *
     * @param yearFile the year file object.
     * @return whether a year file object is a year directory.
     */
    private boolean isYearDirectory(final File yearFile) {
        boolean isYearDirectory = false;
        final String yearFileName = yearFile.getName();
        if (yearFile.isFile()) {
            logger.fine("File " + yearFileName);
        } else if (yearFile.isDirectory()) {
            logger.fine("Directory " + yearFileName);
            // Confirm whether yearFileName is a 4 digit number or not.
            if (Pattern.compile("[0-9][0-9][0-9][0-9]").matcher(yearFileName).matches()) {
                isYearDirectory = true;
            }
        }
        return isYearDirectory;
    }

    /**
     * Retrieve the month directories in the year directory.
     *
     * @param yearDirectory the year directory in which to search for month directories.
     * @return the list of month directories.
     */
    private List<File> getMonthDirectories(final File yearDirectory) {
        final List<File> monthDirectories = new ArrayList<>();
        final File[] monthFiles = yearDirectory.listFiles();
        if (monthFiles != null) {
            for (final File monthFile : monthFiles) {
                if (monthFile.isDirectory() && MONTH_DIRS.contains(monthFile.getName())) {
                    monthDirectories.add(monthFile);
                }
            }
        }
        return monthDirectories;
    }

    /**
     * Retrieve the MS run directories in the month directory.
     *
     * @param monthDirectory the month directory in which to search for MS run directories.
     * @return the list of MS run directories.
     */
    private List<File> getMsRunDirectories(final File monthDirectory) {
        final ArrayList<File> msRunDirectories = new ArrayList<>();
        final File[] msRunFiles = monthDirectory.listFiles();
        if (msRunFiles != null) {
            for (final File msRunFile : msRunFiles) {
                if (msRunFile.isDirectory()) {
                    msRunDirectories.add(msRunFile);
                }
            }
        }
        return msRunDirectories;
    }

    /**
     * Create a report unit and fill it with data from an array of files.
     *
     * We use two specific files:
     * 1) metrics.json: String file containing values of all QC metrics in json object format
     * e.g. {"generic": {"date": "2012/Nov/07 - 14:18", "ms2_spectra": ["MS2 Spectra", "22298 (22298)"],
     * "runtime": "0:16:23", "f_size": ["File Size (MB)", "830.9"],
     * "ms1_spectra": ["MS1 Spectra", "7707 (7707)"]}}
     * 2) msrun*_ticmatrix.csv: CSV file containing x and y axis values for drawing ticGraph
     *
     * @param msrunName the name of the msrun.
     * @param dataFiles the files used to initialize the report unit.
     * @param errorFlag whether an error occurred while reading the files.
     * @return the new report unit.
     */
    private ReportUnit createReportUnit(final String msrunName, final File[] dataFiles, final boolean errorFlag) {
        currentReportNum++;
        logger.fine("Creating report unit No. " + currentReportNum + " for msrun " + msrunName);
        final ReportUnit reportUnit = new ReportUnit(msrunName, currentReportNum);
        reportUnit.setErrorFlag(errorFlag); 
        for (final File dataFile : dataFiles) {
            final String dataFileName = dataFile.getName();
            if (dataFile.isFile()) {
                logger.fine("File " + dataFileName);
                if (dataFileName.equals("metrics.json")) {
                    reportUnit.setMetricsValues(jsonMetricsReader.readJsonValues(dataFile));
                } else if (dataFileName.endsWith("_ticmatrix.csv")) {
                    reportUnit.createChartUnit(readXYSeries(msrunName, dataFile));
                }
            } else if (dataFile.isDirectory()) {
                logger.fine("Directory " + dataFileName);
            }
        }
        return reportUnit;
    }

    /**
     * Create XYSeries by reading TIC matrix file that contains X & Y axis values representing TIC graph
     * @param msrunName the name of the msrun
     * @param ticMatrixFile the tic matrix file to read from
     * @return XYSeries
     */
    private XYSeries readXYSeries(final String msrunName, final File ticMatrixFile) {
        final XYSeries series = new XYSeries(msrunName);
        try {
            final BufferedReader bufferedReader = new BufferedReader(new FileReader(ticMatrixFile));
            bufferedReader.readLine(); //skip first line e.g. ms1Spectra,9239
            bufferedReader.readLine(); //skip second line e.g. ms2Spectra,34040
            bufferedReader.readLine(); //skip third line e.g. maxIntensity,7.8563E9
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                final StringTokenizer lineTokenizer = new StringTokenizer(line, ",");
                // The first token is the x value.
                float x = Float.parseFloat(lineTokenizer.nextToken())/60;
                // The second token is the y value.
                float y = Float.parseFloat(lineTokenizer.nextToken());
                series.add(x, y);
            }
            bufferedReader.close();
        } catch (NumberFormatException | IOException e) {
        	logger.log(Level.SEVERE, "Something went wrong while reading graph series data", e);
        }
        return series;
    }
}
