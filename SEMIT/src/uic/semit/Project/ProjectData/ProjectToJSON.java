package uic.semit.Project.ProjectData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import uic.semit.Project.SourceCodeDownloading.DownloadFiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ProjectToJSON {
	public static List<DownloadFiles> projectFiles = new ArrayList<DownloadFiles>();

	// Change file name after making any changes
	static File file2 = new File(
			"./DATA/Collected/Java_Project_SouceCode_SourceForge_test.json");

	// Pretty string applied here.
	static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	static FileWriter fw1;
	static BufferedWriter bw1;

	public static void main(String[] args) throws Exception {
		fw1 = new FileWriter(file2.getAbsoluteFile());
		bw1 = new BufferedWriter(fw1);

		Project newProject = null;

		// File file = new File(
		// "./DATA/Collected/Java_Project_Data_SourceForge.json");
		// FileWriter fw = new FileWriter(file.getAbsoluteFile());

		fw1 = new FileWriter(file2.getAbsoluteFile()); // for source code

		// BufferedWriter bw = new BufferedWriter(fw);
		//
		// bw.write("[");user
		bw1.write("[");

		try (BufferedReader br = new BufferedReader(new FileReader(
				"./DATA/projectNames/Java_ProjectName_Testing.txt"))) {
			for (String projectName; (projectName = br.readLine()) != null;) {

				newProject = new JsonProjectParser()
						.initiliazeProject(projectName);
				String json = gson.toJson(newProject);
				bw1.write(json);
				bw1.write(",");

			}

		}
		// to avoid extra comma at the end of file (this is just a fix, better
		// should be done
		bw1.write("\n{\n}\n");
		// bw.write("]");
		bw1.write("]");
		// bw.close();
		bw1.close();

	}

	public static void writeToFile(DownloadFiles df) {
		String codejson = gson.toJson(df);
		try {

			bw1.write(codejson);
			bw1.write(",");
		}

		catch (Exception e) {
			System.err.println("IOException");
		}
	}

}
