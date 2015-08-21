package AST_JSON;

import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.habelitz.jsobjectizer.unmarshaller.antlrbridge.generated.JavaLexer;
import com.habelitz.jsobjectizer.unmarshaller.antlrbridge.generated.JavaParser;

public class AST_JSON {

	public static JsonElement getJSON(String filePath) {

		ANTLRFileStream input;
		JavaLexer lexer;
		CommonTokenStream tokens;
		JavaParser parser = null;
		JavaParser.javaSource_return javascource_return;
		CommonTree commonTree = null;
		JsonParser jsonParser = new JsonParser();
		JsonElement element = null;
		String jsonCode = null;
		try {
			input = new ANTLRFileStream(filePath);
			lexer = new JavaLexer(input);

			tokens = new CommonTokenStream(lexer);

			parser = new JavaParser(tokens);

			javascource_return = parser.javaSource();

			commonTree = (CommonTree) javascource_return.getTree();
			jsonCode = toJson(parser, commonTree, " ");

			element = jsonParser.parse(jsonCode);

		} catch (IOException e) {
			System.err.println(filePath + " Not Found");
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			System.err.println("Cannot Recognize File" + filePath);
		} catch (Exception e) {
			System.out.println(e + "\n" + "error" + filePath);
		}
		if (element != null) {
			return element;
		} else {
			return null;
		}

	}

	public static String toJson(JavaParser parser, CommonTree commonTree,
			String indent) {
		String json = indent + "{\n" + indent + "\t\"tokenType\":"
				+ commonTree.token.getType() + ",\n" + indent
				+ "\t\"tokenName\":\""
				+ commonTree.token.getText().replaceAll("\"", "\\\\\"") + "\"";

		if (commonTree.getChildren() != null) {
			String comma = "";
			json += ",\n" + indent + "\t\"children\":[";
			for (Object child : commonTree.getChildren()) {
				// assumes child is also CommonTree...
				json += comma + "\n"
						+ toJson(parser, (CommonTree) child, indent + "\t\t");
				comma = ",";
			}
			json += "\n" + indent + "\t]\n";
		} else {
			json += "\n";
		}

		json = json + indent + "}";

		return json;
	}

	public static void main(String[] args) {

		System.out
				.println(AST_JSON
						.getJSON("/Users/kshitijgautam/Google Drive/Coding/Antlr_Tutorial1/src/tutorial/HelloWorld.java"));
	}

}
