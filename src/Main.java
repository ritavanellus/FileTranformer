
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Rita
 */
public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		FileTransformer ft = new FileTransformer(
				"C:\\Users\\Rita\\Documents\\WIFI_Kurs\\MyProject_Orseg\\MAP_FILES\\FILES_W_ALT",
				"C:\\Users\\Rita\\Documents\\WIFI_Kurs\\MyProject_Orseg\\MAP_FILES\\FILES_W_ALT\\filenamesList_All.txt",
				"C:\\Users\\Rita\\Documents\\WIFI_Kurs\\MyProject_Orseg\\MAP_FILES\\FILES_W_ALT\\forMap.txt",
				"C:\\Users\\Rita\\Documents\\WIFI_Kurs\\MyProject_Orseg\\MAP_FILES\\JSON",
				"test_All");

		ft.doTheJob();

//		 ft.getCoordinatesAsArray(stringSource);
//		 ft.generateDistAlt(0);
//		 System.out.println(Arrays.toString(ft.distAltArray[ft.distAltArray.length-1]));	 
	}

}
