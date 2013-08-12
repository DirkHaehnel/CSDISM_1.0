import ij.IJ;
import java.util.Collection;
import java.util.Map;

import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;

public class ImageProcessing {

	/**
	 * @throws ImgIOException
	 * @throws IncompatibleTypeException
	 */
	public <T extends RealType<T> & NativeType<T>> ImageProcessing(
			String reffile, String datafile, long firstFrame,
			double background, int precision, int fwhm, int lenseDistance,
			int pixelSize, Collection<Localizable> peaks,
			Map<Integer, int[]> setOfMetaData, Integer referenceKey,
			int[] rapidPrefs, boolean rapidStorm, boolean sCMOS, boolean debug)
			throws ImgIOException, IncompatibleTypeException {

		int[] metaData = null;

		if (rapidStorm) {
			metaData = rapidPrefs;

		} else {
			metaData = setOfMetaData.get(referenceKey);

		}

		Integer pixelX = metaData[0];
		Integer pixelY = metaData[1];
		Integer pixelZ = metaData[2];
		Integer dimX = metaData[3];
		Integer dimY = metaData[4];
		Integer dimZ = metaData[5];
		Img<LongType> image = new ImgOpener().openImg(datafile,
				new ArrayImgFactory<LongType>(), new LongType());

		if (debug) {
			ImageJFunctions.showFloat(image).setTitle("data");
		}
		final RandomAccess<LongType> r = image.randomAccess();
		int resolutionSpreadParameter = 2;
		int newDimX = resolutionSpreadParameter * precision * pixelX;
		int newDimY = resolutionSpreadParameter * precision * pixelY;

		final int[] dimensions = new int[] { newDimX, newDimY };

		final Img<LongType> newimg = new ArrayImgFactory<LongType>().create(
				dimensions, new LongType());

		final RandomAccess<LongType> rr = newimg.randomAccess();

		final int[] backgroundPixel = new int[] { 1, 1 };
		final Img<LongType> backgroundImage = new ArrayImgFactory<LongType>()
				.create(backgroundPixel, new LongType());
		final RandomAccess<LongType> backgroundSubPixel = backgroundImage
				.randomAccess();
		int lengthcounter = peaks.size();
		int counter = 0;
		int jumpStart = -55;
		int dataJumpStart = -5;
		int windowLength = 110;
		int window = 11;
		long startX;
		long startY;
		int subIntervalEndX;
		int subIntervalStartX;
		int subIntervalEndY;
		int subIntervalStartY;
		long shiftDecimal = 1000;
		for (Localizable peak : peaks) {
			counter++;
			IJ.showProgress(counter, lengthcounter);

			Localizable imgCoordinate = new Point((resolutionSpreadParameter
					* precision * (peak.getLongPosition(dimX) / shiftDecimal)),
					(resolutionSpreadParameter * precision
							* (peak.getLongPosition(dimY)) / shiftDecimal));
			rr.setPosition(imgCoordinate);

			Localizable peakCoordinate = new Point(
					(peak.getLongPosition(dimX) / shiftDecimal),
					(peak.getLongPosition(dimY) / shiftDecimal),
					(peak.getLongPosition(dimZ) / shiftDecimal));
			r.setPosition(peakCoordinate);

			if ((peakCoordinate.getLongPosition(dimZ) / shiftDecimal) <= (pixelZ)) {

				rr.move(jumpStart, dimY);
				r.move(dataJumpStart, dimY);
				rr.move(jumpStart, dimX);
				r.move(dataJumpStart, dimX);
				startX = rr.getLongPosition(dimX);
				startY = rr.getLongPosition(dimY);

				subIntervalEndX = (int) (startX % precision);
				subIntervalStartX = (int) (subIntervalEndX - (startX % precision));
				subIntervalEndY = (int) (startY % precision);
				subIntervalStartY = (int) (subIntervalEndY - (startY % precision));

				for (int y = 1; y < (windowLength); y++) {

					if (y > 1) {

						rr.move((-1) * (windowLength), dimX);

						r.move((-1) * (window), dimX);

						rr.fwd(dimY);

					}

					if (y % precision == 0) {
						r.fwd(dimY);
					}
					if (y == subIntervalStartY) {
						r.fwd(dimY);
					}
					if (y == subIntervalEndY) {
						r.fwd(dimY);
					}

					for (int x = 1; x < (windowLength); x++) {

						if (x % precision == 0) {
							r.fwd(dimX);
						}
						if (x == subIntervalStartX) {
							r.fwd(dimX);
						}
						if (x == subIntervalEndX) {
							r.fwd(dimX);
						}

						LongType bb = backgroundSubPixel.get();
						bb.set(100);
						LongType tt = rr.get();
						LongType t = r.get();
						tt.add(t);
						tt.sub(bb);

						rr.fwd(dimX);

					}

				}
			}
		}

		ImageJFunctions.showFloat(newimg, "SuperHighImage");

	}

}
