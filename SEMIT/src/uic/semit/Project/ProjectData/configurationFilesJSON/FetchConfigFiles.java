package uic.semit.Project.ProjectData.configurationFilesJSON;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.XML;

import uic.semit.kshitij.FileUtils;
import uic.semit.kshitij.FileWalker;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class FetchConfigFiles {

	public static ConfigFiles getConfigFiles(String projectName) {

		if (new File(FileWalker.codePath + projectName).isDirectory()) {
			String extension = "xml";
			List<File> SourceCodeFiles = FileWalker
					.getFilesFromProjectWithExtension(projectName, extension);

			List<ConfigurationFile> codeFiles = new ArrayList<>();
			ConfigFiles projectFiles = new ConfigFiles();

			List<ConfigFile> ConfigCodeFilesSVN = new ArrayList<>();
			List<ConfigFile> ConfigCodeFilesGIT = new ArrayList<>();
			List<ConfigFile> ConfigCodeFilesCVS = new ArrayList<>();
			List<ConfigFile> ConfigCodeFilesBZR = new ArrayList<>();
			List<ConfigFile> ConfigCodeFilesHG = new ArrayList<>();
			List<ConfigFile> ConfigCodeFilesCODE = new ArrayList<>();

			for (File codeFile : SourceCodeFiles) {
				if (codeFile.exists()) {
					if (codeFile.getPath()
							.contains("/" + projectName + "/svn/")) {

						ConfigCodeFilesSVN.add(new ConfigFile(codeFile
								.getName(), codeFile.getPath(), extension,
								codeFile.length(), getNumberOfLines(codeFile),
								XMLtoJSON(codeFile.getPath())));

					} else if (codeFile.getPath().contains(
							"/" + projectName + "/git/")) {
						ConfigCodeFilesGIT.add(new ConfigFile(codeFile
								.getName(), codeFile.getPath(), extension,
								codeFile.length(), getNumberOfLines(codeFile),
								XMLtoJSON(codeFile.getPath())));

					} else if (codeFile.getPath().contains(
							"/" + projectName + "/hg/")) {
						ConfigCodeFilesHG.add(new ConfigFile(
								codeFile.getName(), codeFile.getPath(),
								extension, codeFile.length(),
								getNumberOfLines(codeFile), XMLtoJSON(codeFile
										.getPath())));

					} else if (codeFile.getPath().contains(
							"/" + projectName + "/bzr/")) {
						ConfigCodeFilesBZR.add(new ConfigFile(codeFile
								.getName(), codeFile.getPath(), extension,
								codeFile.length(), getNumberOfLines(codeFile),
								XMLtoJSON(codeFile.getPath())));

					} else if (codeFile.getPath().contains(
							"/" + projectName + "/cvs/")) {
						ConfigCodeFilesCVS.add(new ConfigFile(codeFile
								.getName(), codeFile.getPath(), extension,
								codeFile.length(), getNumberOfLines(codeFile),
								XMLtoJSON(codeFile.getPath())));

					} else {
						ConfigCodeFilesCODE.add(new ConfigFile(codeFile
								.getName(), codeFile.getPath(), extension,
								codeFile.length(), getNumberOfLines(codeFile),
								XMLtoJSON(codeFile.getPath())));

					}

				}
			}
			/*
			 * End of For Loop.
			 */

			if (ConfigCodeFilesSVN.size() != 0) {

				codeFiles.add(new ConfigurationFile("SVN", ConfigCodeFilesSVN));

			}

			if (ConfigCodeFilesGIT.size() != 0) {
				codeFiles.add(new ConfigurationFile("GIT", ConfigCodeFilesGIT));
			}

			if (ConfigCodeFilesHG.size() != 0) {
				codeFiles.add(new ConfigurationFile("HG", ConfigCodeFilesHG));
			}

			if (ConfigCodeFilesCVS.size() != 0) {
				codeFiles.add(new ConfigurationFile("CVS", ConfigCodeFilesCVS));
			}

			if (ConfigCodeFilesBZR.size() != 0) {
				codeFiles.add(new ConfigurationFile("BZR", ConfigCodeFilesBZR));
			}

			if (ConfigCodeFilesCODE.size() != 0) {
				codeFiles
						.add(new ConfigurationFile("CODE", ConfigCodeFilesCODE));
			}
			projectFiles.setFiles(codeFiles);

			return projectFiles;

		} else {
			return null;
		}
	}

	public static int getNumberOfLines(File file) {
		LineNumberReader lnr;
		int totalLines = 0;
		try {
			lnr = new LineNumberReader(new FileReader(file));
			lnr.skip(Long.MAX_VALUE);
			totalLines = lnr.getLineNumber() + 1;
			lnr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("File Not Found for counting Lines: "
					+ file.getPath() + ": FetchSourceFiles.java");
		}

		// Add 1 because line index starts at 0
		// Finally, the LineNumberReader object should be closed to prevent
		// resource leak

		return totalLines;
	}

	public static JsonElement XMLtoJSON(String xmlFilepath) {
		JSONObject json = null;
		JsonParser jsonParser = new JsonParser();
		JsonElement element = null;
		String jsonString = null;
		try {
			json = XML.toJSONObject(FileUtils.readTextFile(xmlFilepath));
			jsonString = json.toString(4);
			element = jsonParser.parse(jsonString);
		} catch (Exception e) {
			System.out.println(e + "----" + xmlFilepath);
		}

		// sending back pretty string with indendation 4
		if (element != null) {
			return element;
		} else {
			return null;
		}

	}

	public static void main(String[] args) {

		ConfigFiles sf = FetchConfigFiles.getConfigFiles("smyle");
		List<ConfigurationFile> cf = sf.getFiles();

		System.out.println(cf.size());
		List<ConfigFile> sc = cf.get(0).getSourceCode();
		System.out.println(sc.size());
		System.out.println(cf.get(0).getRepository());
		for (ConfigFile a : sc) {
			// System.out.println(a.getCodeJson());

		}
		ConfigFiles sf1 = FetchConfigFiles.getConfigFiles("asdasds");
		System.out.println(sf1);

	}

}