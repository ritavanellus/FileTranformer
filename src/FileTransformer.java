
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Rita
 */

/*
source of json import: https://code.google.com/archive/p/json-simple/downloads
file: json-simple-1.1.1.jar
 */
public class FileTransformer {

	public String sourceFolderPathString;
	public List<String> filesToHandle;
	public String sourceListPathString;
	public String mapFilePathString;
	public String targetFolderAsString;
	public String jsonName;
	public List<Double> longList;
	public List<Double> latList;
	public List<Double> altList;
	public double[][] distAltArray;
	public double[][] polyLineData;
	public boolean isAsphalt;
	public double elevation;
	public HashMap pointDefs;

	public FileTransformer(String sourceFolder, String fileListPath, String mapFile, String target, String json) {
		this.sourceFolderPathString = sourceFolder;
		this.sourceListPathString = fileListPath;
		this.mapFilePathString = mapFile;
		this.targetFolderAsString = target;
		this.jsonName = json;
	}

	/* Goal: need a file that can be read in in JS, to provide data points for the elevation chart
	 * Contains info on the plotlines, too - start and end plotline 
	 * Strategy: create JSON, parameters: startLine, endLine, data 
	 * data should be arranged as a 2D array, inner arrays are x-y data pairs, distance-elevation 
	 * unnecessary points (no change in elevation) should be eliminated to spare data points.
	 */
	public void doTheJob() {
		filesToHandle = new ArrayList<String>();
		filesToHandle = readInFileList();
		String fullTarget = targetFolderAsString + "\\" + jsonName + ".json";
		Path targetPath = Paths.get(fullTarget);
		pointDefs = createMapFromFile();
		JSONObject mainObj = new JSONObject();

		for (Iterator<String> iter = filesToHandle.iterator(); iter.hasNext();) {

			String file = iter.next();
			String fullActualPath = sourceFolderPathString + "\\" + file + ".kml";
			//no need to initialize the lists/arrays, they are always new initialized in the appropriate methods
			List<Double>[] result = getCoordinatesAsArray(fullActualPath);
			longList = result[0];
			latList = result[1];
			altList = result[2];
			//here one can give a threshold to discard points with no relevant alt change - still better to kepp at zero
			distAltArray = generateDistAlt(0);
			polyLineData = generatePolyLineTrace(0);

			JSONObject obj = new JSONObject();
			obj.put("startPoint", pointDefs.get(file.substring(0, 3)));
			obj.put("endPoint", pointDefs.get(file.substring(4, 7)));
			obj.put("length", distAltArray[distAltArray.length - 1][0]);

			isAsphalt = file.substring(11, 12).equals("A");
			obj.put("asphalt", isAsphalt);

			elevation = calculateElevation();
			obj.put("elevation", elevation);

			JSONArray distAlt = new JSONArray();
			//check if Alt value is 0, then skip it!
			for (double[] subArray : distAltArray) {
				if (subArray[1]==0){
					continue;
				}
				JSONObject xyPair = new JSONObject();
				xyPair.put("x", subArray[0]);
				xyPair.put("y", subArray[1]);
				distAlt.add(xyPair);
			}
			obj.put("forChart", distAlt);

			JSONArray polyLine = new JSONArray();
			for (double[] subArray : polyLineData) {
				JSONObject latLng = new JSONObject();
				latLng.put("lat", subArray[0]);
				latLng.put("lng", subArray[1]);
				polyLine.add(latLng);
			}
			obj.put("polyLine", polyLine);

			mainObj.put(file.substring(0, 10), obj);

		}
		try (FileWriter fw = new FileWriter(targetPath.toFile(), true)) {

			fw.write(mainObj.toJSONString());
			fw.flush();

		} catch (IOException e) {
			System.out.println("Watch out, something went wrong!");
			e.printStackTrace();
		}

	}

	public List<String> readInFileList() {
		Path sourceFilesPath = Paths.get(this.sourceListPathString);
		List<String> fileList = new ArrayList<String>();

		try (BufferedReader br = new BufferedReader(new FileReader(sourceFilesPath.toFile()))) {
			String line;

			while ((line = br.readLine()) != null) {
				fileList.add(line);
			}

		} catch (FileNotFoundException ex) {
			Logger.getLogger(FileTransformer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(FileTransformer.class.getName()).log(Level.SEVERE, null, ex);
		}
		return fileList;
	}

	//return value is an array of 3 lists
	public List<Double>[] getCoordinatesAsArray(String source) {
		Path sourcePath = Paths.get(source);
		StringBuilder sb = new StringBuilder();

		try (BufferedReader br = new BufferedReader(new FileReader(sourcePath.toFile()))) {
			String line;

			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (FileNotFoundException ex) {
			Logger.getLogger(FileTransformer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(FileTransformer.class.getName()).log(Level.SEVERE, null, ex);
		}

		int LineStringStart = sb.indexOf("<LineString>");
		int LineStringEnd = sb.indexOf("</LineString>");

		String bwLineStrings = sb.substring(LineStringStart, LineStringEnd);

		int coordStart = bwLineStrings.indexOf("<coordinates>");
		int coordEnd = bwLineStrings.indexOf("</coordinates>");

		String coordAll = bwLineStrings.substring(coordStart + 13, coordEnd);
		String coordReplaced = coordAll.replace(" ", ",");

		//now this is an array containing series of long-lat-alt data
		String[] allInArray = coordReplaced.split(",");

		//sorting elements into lists
		longList = new ArrayList<Double>();
		latList = new ArrayList<Double>();
		altList = new ArrayList<Double>();
		int pos = 0;

		while (pos < allInArray.length) {

			longList.add(Double.parseDouble(allInArray[pos++]));
			latList.add(Double.parseDouble(allInArray[pos++]));
			altList.add(Double.parseDouble(allInArray[pos++]));
		}

		List<Double>[] result = new List[]{longList, latList, altList};

		return result;
	}

	//points with no relevant change in alt are discarded (change<smoothThreshold is considered irrelevant)
	public double[][] generateDistAlt(int smoothThreshold) {

		double dist = 0;
		double oldX = 0;
		double oldY = 0;
		double newX;
		double newY;
		double deltaX;
		double deltaY;
		double deltaXinKm;
		double deltaYinKm;
		double altLevel = 0;

		int pos = 0;
		int N = longList.size();
		distAltArray = new double[N][2];

		while (pos < N) {
			if (pos == 0) {
				distAltArray[0][0] = 0;
				double firstAlt = altList.get(0);
				distAltArray[0][1] = firstAlt;
				altLevel = firstAlt;
				oldX = longList.get(0);
				oldY = latList.get(0);
				pos++;
			} else {
				double currAlt = altList.get(pos);
				newX = longList.get(pos);
				newY = latList.get(pos);
				deltaX = newX - oldX;
				deltaY = newY - oldY;
				deltaXinKm = deltaX * 76.19;
				deltaYinKm = deltaY * 111.18;
				double currDist = Math.sqrt(Math.pow(deltaXinKm, 2) + Math.pow(deltaYinKm, 2));
				dist += currDist;
				distAltArray[pos][0] = dist;

				//case: no relevant change in Alt - set Alt to 0 in the subarray
				if (currAlt >= altLevel - smoothThreshold && currAlt <= altLevel + smoothThreshold) {
					distAltArray[pos][1] = 0;
				} else {
					distAltArray[pos][1] = currAlt;
					altLevel = currAlt;
				}
				oldX = newX;
				oldY = newY;
				pos++;
			}
		}
		return distAltArray;
	}

	//so far the order was long-lat, but here one needs lat-long pairs
	//later it will be appended to filter out important coordinates (that's why the smoothThreshold is introduced)
	public double[][] generatePolyLineTrace(int smoothThreshold) {

		int N = longList.size();
		polyLineData = new double[N][2];

		for (int pos = 0; pos < N; pos++) {
			polyLineData[pos][0] = latList.get(pos);
			polyLineData[pos][1] = longList.get(pos);
		}

		return polyLineData;
	}

	public double calculateElevation() {
		double sumEl = 0;
		double actual;
		double next;

		for (int pos = 0; pos < altList.size() - 1; pos++) {
			actual = altList.get(pos);
			next = altList.get(pos + 1);
			if (next > actual) {
				sumEl += next - actual;
			}
		}
		return sumEl;
	}

	public HashMap createMapFromFile() {
		HashMap<String, String> map = new HashMap<String, String>();

		Path sourcePath = Paths.get(mapFilePathString);

		try (BufferedReader br = new BufferedReader(new FileReader(sourcePath.toFile()))) {
			String line;

			while ((line = br.readLine()) != null) {				
				String[] splitted = line.split(",");				
				map.put(splitted[0], splitted[1]);
			}

		} catch (FileNotFoundException ex) {
			Logger.getLogger(FileTransformer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(FileTransformer.class.getName()).log(Level.SEVERE, null, ex);
		}
		return map;
	}
}
