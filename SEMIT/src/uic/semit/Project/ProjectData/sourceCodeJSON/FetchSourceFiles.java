package uic.semit.Project.ProjectData.sourceCodeJSON;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uic.semit.kshitij.FileWalker;
import AST_JSON.AST_JSON;

public class FetchSourceFiles {

	public static SourceFiles getSourceFiles(String projectName) {

		String extension = ".java";
		List<File> SourceCodeFiles = FileWalker
				.getFilesFromProjectWithExtension(projectName, extension);

		List<CodeFile> codeFiles = new ArrayList<>();
		SourceFiles projectFiles = new SourceFiles();

		HashMap<String, List<SourceCode>> repoMapWithCodeFiles = new HashMap<String, List<SourceCode>>();

		List<SourceCode> SourceCodeFilesSVN = new ArrayList<>();
		List<SourceCode> SourceCodeFilesGIT = new ArrayList<>();
		List<SourceCode> SourceCodeFilesCVS = new ArrayList<>();
		List<SourceCode> SourceCodeFilesBZR = new ArrayList<>();
		List<SourceCode> SourceCodeFilesHG = new ArrayList<>();
		List<SourceCode> SourceCodeFilesCODE = new ArrayList<>();

		for (File codeFile : SourceCodeFiles) {
			if (codeFile.exists()) {
				if (codeFile.getName().contains("/svn/")) {

					SourceCodeFilesSVN.add(new SourceCode(codeFile.getName(),
							codeFile.getPath(), extension, codeFile.length(),
							getNumberOfLines(codeFile), AST_JSON
									.getJSON(codeFile.getPath())));

				} else if (codeFile.getName().contains("/git/")) {
					SourceCodeFilesGIT.add(new SourceCode(codeFile.getName(),
							codeFile.getPath(), extension, codeFile.length(),
							getNumberOfLines(codeFile), AST_JSON
									.getJSON(codeFile.getPath())));

				} else if (codeFile.getName().contains("/hg/")) {
					SourceCodeFilesCVS.add(new SourceCode(codeFile.getName(),
							codeFile.getPath(), extension, codeFile.length(),
							getNumberOfLines(codeFile), AST_JSON
									.getJSON(codeFile.getPath())));

				} else if (codeFile.getName().contains("/bzr/")) {
					SourceCodeFilesBZR.add(new SourceCode(codeFile.getName(),
							codeFile.getPath(), extension, codeFile.length(),
							getNumberOfLines(codeFile), AST_JSON
									.getJSON(codeFile.getPath())));

				} else if (codeFile.getName().contains("cvs")) {
					SourceCodeFilesHG.add(new SourceCode(codeFile.getName(),
							codeFile.getPath(), extension, codeFile.length(),
							getNumberOfLines(codeFile), AST_JSON
									.getJSON(codeFile.getPath())));

				} else {
					SourceCodeFilesCODE.add(new SourceCode(codeFile.getName(),
							codeFile.getPath(), extension, codeFile.length(),
							getNumberOfLines(codeFile), AST_JSON
									.getJSON(codeFile.getPath())));

				}

			}
		}
		/*
		 * End of For Loop.
		 */

		if (SourceCodeFilesSVN.size() != 0) {

			codeFiles.add(new CodeFile("SVN", SourceCodeFilesSVN));

		}
		if (SourceCodeFilesGIT.size() != 0) {
			codeFiles.add(new CodeFile("GIT", SourceCodeFilesGIT));
		}

		if (SourceCodeFilesHG.size() != 0) {
			codeFiles.add(new CodeFile("HG", SourceCodeFilesHG));
		}

		if (SourceCodeFilesCVS.size() != 0) {
			codeFiles.add(new CodeFile("CVS", SourceCodeFilesCVS));
		}

		if (SourceCodeFilesBZR.size() != 0) {
			codeFiles.add(new CodeFile("BZR", SourceCodeFilesBZR));
		}

		if (SourceCodeFilesCODE.size() != 0) {
			codeFiles.add(new CodeFile("CODE", SourceCodeFilesCODE));
		}
		projectFiles.setFiles(codeFiles);
		return projectFiles;
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
			System.err
					.println("File Not Found for counting Lines: FileName: FetchSourceFiles.java");
		}

		// Add 1 because line index starts at 0
		// Finally, the LineNumberReader object should be closed to prevent
		// resource leak

		return totalLines;
	}

	public static void main(String[] args) {

		System.out.println(FetchSourceFiles.getSourceFiles("smyle").getFiles()
				.size());

	}

}
