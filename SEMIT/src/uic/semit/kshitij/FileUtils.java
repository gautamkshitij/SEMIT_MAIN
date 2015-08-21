package uic.semit.kshitij;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
	public static String readTextFile(String fileName) {
		String content = "";
		try {
			content = new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e + "\n" + fileName);
		}
		if (content.length() != 0)
			return content;
		else
			return null;
	}

	public static List<String> readTextFileByLines(String fileName)
			throws IOException {

		List<String> lines = null;
		// = Files.readAllLines(Paths.get(fileName));
		return lines;
	}

	public static void writeToTextFile(String fileName, String content)
			throws IOException {
		Files.write(Paths.get(fileName), content.getBytes(),
				StandardOpenOption.CREATE);
	}

	public static List<File> dirTree(File dir) {
		List<File> fileTree = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				if (file.getName().endsWith("java")) {
					fileTree.add(file);
					System.out.println(file);
				}
			} else {
				dirTree(dir);

			}
		}
		return fileTree;
	}

	public static void main(String[] args) throws IOException {

		// String input = FileUtils.readTextFile("./input.txt");
		// System.out.println(input);
		// // FileUtils.writeToTextFile("copy.txt", input);

		// System.out.println(FileUtils.readTextFileByLines("./input.txt"));

		// FileUtils.readTextFileByLines("./input.txt");
		// Path path = Paths.get("./input.txt");

		File dir = new File(
				"/Users/kshitijgautam/Google Drive/Coding/workspace/SEMIT_MAIN/DATA/CODE/smyle");
		dirTree(dir);

	}
}
