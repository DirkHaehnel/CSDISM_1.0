import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.io.ImgIOException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * LoadReferenceData
 */
public class LoadReferenceData {

	@SuppressWarnings({ "null", "unused" })
	public <T extends RealType<T> & NativeType<T>> LoadReferenceData(
			String reffile, Collection<Localizable> peaks,
			Map<Integer, int[]> setOfMetaData, Integer referenceKey,
			int corner, int[] rapidPrefs, boolean rapidStorm, boolean debug)
			throws ImgIOException, IncompatibleTypeException, IOException {

		InputStream referenceInputStream = null;
		DataInputStream referenceDataInputstream = null;
		long shiftDecimal = 1000;
		if (!rapidStorm) {
			try {
				referenceInputStream = new FileInputStream(reffile);
				referenceDataInputstream = new DataInputStream(
						referenceInputStream);

				int pixelX = (int) referenceDataInputstream.readDouble();
				int pixelY = (int) referenceDataInputstream.readDouble();
				int pixelZ = (int) referenceDataInputstream.readDouble();

				int dimx = (int) referenceDataInputstream.readDouble();
				int dimy = (int) referenceDataInputstream.readDouble();
				int dimz = (int) referenceDataInputstream.readDouble();

				int[] metaData = new int[] { pixelX, pixelY, pixelZ, dimx,
						dimy, dimz };
				setOfMetaData.put(referenceKey, metaData);

				while (referenceDataInputstream.available() > 0) {
					double x = referenceDataInputstream.readDouble()
							* shiftDecimal;
					double y = referenceDataInputstream.readDouble()
							* shiftDecimal;
					double z = referenceDataInputstream.readDouble()
							* shiftDecimal;
					Localizable coordinate = new Point((long) x, (long) y,
							(long) z);

					if ((coordinate.getLongPosition(dimx) / shiftDecimal) > corner
							&& (coordinate.getLongPosition(dimx) / shiftDecimal) < (pixelX - corner)
							&& (coordinate.getLongPosition(dimy) / shiftDecimal) > corner
							&& (coordinate.getLongPosition(dimy) / shiftDecimal) < (pixelY - corner)) {
						peaks.add(coordinate);
					}
				}
			} catch (Exception e) {

				e.printStackTrace();
			} finally {
				if (referenceInputStream != null)
					try {
						referenceInputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				if (referenceDataInputstream != null)
					try {
						referenceDataInputstream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		} else if (rapidStorm) {
			try {
				FileInputStream fileInputStreamRapidStorm = new FileInputStream(
						reffile);
				BufferedReader bufferedInputReaderRapidstorm = new BufferedReader(
						new InputStreamReader(fileInputStreamRapidStorm));

				String lineFromFile;
				int pixelX = rapidPrefs[0];
				int pixelY = rapidPrefs[1];
				int pixelZ = rapidPrefs[2];
				int dimx = rapidPrefs[3];
				int dimy = rapidPrefs[4];
				int dimz = rapidPrefs[5];

				while ((lineFromFile = bufferedInputReaderRapidstorm.readLine()) != null) {

					String[] lineResult = lineFromFile.split(" ");

					double coordinateX = Double.parseDouble(lineResult[0]);
					double coordinateY = Double.parseDouble(lineResult[1]);
					double coordinateZ = Double.parseDouble(lineResult[2]);

					double x = coordinateX * (shiftDecimal / 100);
					double y = coordinateY * (shiftDecimal / 100);
					double z = coordinateZ * shiftDecimal;
					Localizable coordinate = new Point((long) x, (long) y,
							(long) z);

					if (((coordinateX / shiftDecimal) > corner)
							&& (coordinateX / (shiftDecimal / 10)) < (pixelX - corner)
							&& (coordinateY / (shiftDecimal / 10)) > corner
							&& (coordinateZ / shiftDecimal) < (pixelY - corner)) {
						peaks.add(coordinate);
					}
				}
				fileInputStreamRapidStorm.close();
				bufferedInputReaderRapidstorm.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
