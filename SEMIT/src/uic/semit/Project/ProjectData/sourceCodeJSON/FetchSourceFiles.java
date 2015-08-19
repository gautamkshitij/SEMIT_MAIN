package uic.semit.Project.ProjectData.sourceCodeJSON;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

public class FetchSourceFiles {

	public static SourceFiles getSourceFiles(String projectName) {
		String ProjectPath = "/Users/kshitijgautam/Google Drive/Coding/workspace/SEMIT_MAIN/DATA/"
				+ projectName + "/";
		System.out.println(ProjectPath);
		System.out.println(getAllFileswithExtension("java", ProjectPath));
		return null;

	}

	public static List<File> getAllFileswithExtension(String extension,
			String path) {

		File dir = new File(path);
		System.out.println(dir.listFiles().toString());

		return null;

	}

	public static void main(String[] args) {

		System.out.println(FetchSourceFiles.getSourceFiles("jjicalc")
				.toString());
		System.out.println(FetchSourceFiles.getSourceFiles("cachier")
				.toString());
		System.out.println(FetchSourceFiles.getSourceFiles("smyle").toString());
	}

}