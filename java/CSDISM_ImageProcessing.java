import ij.*;
import ij.gui.*;
import ij.io.OpenDialog;
import ij.plugin.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.imglib2.Localizable;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.io.ImgIOException;

public class CSDISM_ImageProcessing implements PlugIn {

	@SuppressWarnings("null")
	@Override
	public void run(String arg) {
		try {
			int[] rapidPrefs = null;
			int digits = 0;
			double prefFirstFrame = 1;
			double prefBackground = 100;
			double prefPrecision = 10;
			double prefFwhm = 300;
			double prefLenseDistance = 10;
			double prefPixelSize = 60;
			boolean prefSCMOS = false;
			boolean prefRapidStorm = false;
			boolean prefDebug = false;
			String data_dir = null;
			String data_file = null;
			String ref_dir = null;
			String ref_file = null;

			GenericDialog imageProcessingSettings = new GenericDialog(
					"CSDISM Image Processing Settings");
			imageProcessingSettings.addNumericField("Start Frame:",
					prefFirstFrame, digits);
			imageProcessingSettings.addNumericField("Background:",
					prefBackground, digits);
			imageProcessingSettings.addNumericField("Precision Factor:",
					prefPrecision, digits);
			imageProcessingSettings.addNumericField("FWHM (ym):", prefFwhm,
					digits);
			imageProcessingSettings.addNumericField("Lense Distance (ym) ",
					prefLenseDistance, digits);
			imageProcessingSettings.addNumericField("Pixelsize (ym):",
					prefPixelSize, digits);
			imageProcessingSettings.addCheckbox("SCMOS", prefSCMOS);
			imageProcessingSettings.addCheckbox("RapidStorm", prefRapidStorm);
			imageProcessingSettings.addCheckbox("Show Imaging Prozess (Slow!)",
					prefDebug);
			imageProcessingSettings.showDialog();

			if (imageProcessingSettings.wasCanceled())
				return;
			long firstFrame = (long) imageProcessingSettings.getNextNumber();
			double background = imageProcessingSettings.getNextNumber();
			int precision = (int) imageProcessingSettings.getNextNumber();
			int fwhm = (int) imageProcessingSettings.getNextNumber();
			int lenseDistance = (int) imageProcessingSettings.getNextNumber();
			int pixelSize = (int) imageProcessingSettings.getNextNumber();
			boolean sCMOS = imageProcessingSettings.getNextBoolean();
			boolean rapidStorm = imageProcessingSettings.getNextBoolean();
			boolean debug = imageProcessingSettings.getNextBoolean();

			OpenDialog chooser_ref = new OpenDialog(
					"Choose Reference Data File for CSDISM", ref_dir, ref_file);
			String reffile = chooser_ref.getDirectory()
					+ chooser_ref.getFileName();

			OpenDialog chooser_data = new OpenDialog(
					"Choose Data File for CSDISM", data_dir, data_file);
			String datafile = chooser_data.getDirectory()
					+ chooser_data.getFileName();
			;

			if (rapidStorm) {
				GenericDialog rapidStormSettings = new GenericDialog(
						"Information of Rapidstorm Data");
				double prefPixelsX = 512;
				rapidStormSettings.addNumericField("Pixels X:", prefPixelsX,
						digits);
				double prefPixelsY = 512;
				rapidStormSettings.addNumericField("Pixels Y:", prefPixelsY,
						digits);
				double prefPixelsZ = 512;
				rapidStormSettings.addNumericField("Pixels Z:", prefPixelsZ,
						digits);
				double prefAxisX = 0;
				rapidStormSettings
						.addNumericField("Axis X:", prefAxisX, digits);
				double prefAxisY = 1;
				rapidStormSettings
						.addNumericField("Axis Y:", prefAxisY, digits);
				double prefAxisZ = 2;
				rapidStormSettings
						.addNumericField("Axis Z:", prefAxisZ, digits);
				rapidStormSettings.showDialog();
				if (rapidStormSettings.wasCanceled()) {
					return;
				}

				int pixelX = (int) rapidStormSettings.getNextNumber();
				int pixelY = (int) rapidStormSettings.getNextNumber();
				int pixelZ = (int) rapidStormSettings.getNextNumber();
				int dimx = (int) rapidStormSettings.getNextNumber();
				int dimy = (int) rapidStormSettings.getNextNumber();
				int dimz = (int) rapidStormSettings.getNextNumber();
				rapidPrefs = new int[] { pixelX, pixelY, pixelZ, dimx, dimy,
						dimz };

			}

			int numOfSpots = 1024;
			Integer referenceKey = 1;
			int corner = 5;
			Collection<Localizable> peaks = new HashSet<Localizable>(numOfSpots);
			int numOfMetaData = 16;

			Map<Integer, int[]> setOfMetaData = new HashMap<Integer, int[]>(
					numOfMetaData);

			new LoadReferenceData(reffile, peaks, setOfMetaData, referenceKey,
					corner, rapidPrefs, rapidStorm, debug);

			new ImageProcessing(reffile,datafile, firstFrame, background,
					precision, fwhm, lenseDistance, pixelSize, peaks,
					setOfMetaData, referenceKey,rapidPrefs,rapidStorm, sCMOS, debug);
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		new WaitForUserDialog(
				"Image Processing Finished!  -  Click OK, to continue").show();

	}

	public static void main(String[] args) throws ImgIOException,
			IncompatibleTypeException, IOException {

		// set the plugins.dir property to make the plugin appear in the Plugins
		// menu
		Class<?> clazz = CSDISM_ImageProcessing.class;
		String url = clazz.getResource(
				"/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length()
				- clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);
		// open an ImageJ window
		new ImageJ();
		// run the example
		IJ.runPlugIn(clazz.getName(), "");

	}

}
