package uic.semit.kshitij;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileWalker {
	static List<File> listofFiles = null;
	public static String codePath = "/Users/kshitijgautam/Google Drive/Coding/workspace/SEMIT_MAIN/DATA/CODE/";

	public void walk(String path, String extension) {

		File root = new File(path);

		for (File f : root.listFiles()) {

			if (f.isDirectory()) {

				walk(f.getAbsolutePath(), extension);

			}

			else {

				if (f.getName().endsWith(extension)) {

					listofFiles.add(f);
					//					System.out.println("File:" + f.getAbsoluteFile());

				}
			}
		}

	}

	public static List<File> getFilesFromProjectWithExtension(
			String projectName, String extension) {
		listofFiles = new ArrayList<>();
		FileWalker fw = new FileWalker();

		fw.walk(codePath + projectName, extension);
		return listofFiles;
	}

	// public static void main(String[] args) {
	// System.out
	// .println(FileWalker.getFilesWithExtension("cachier", ".java"));
	// System.out.println(FileWalker.getFilesWithExtension("smyle", ".java"));
	// }

}