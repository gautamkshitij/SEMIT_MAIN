package uic.semit.Project.ProjectData.configurationFilesJSON;

import javax.annotation.Generated;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class ConfigFile {

	@Expose
	private String fileName;
	@Expose
	private String absFilePath;
	@Expose
	private String fileType;
	@Expose
	private long fileSize;
	@Expose
	private Integer linesOfCode;
	@SerializedName("code_json")
	@Expose
	private JsonElement codeJson;

	public ConfigFile(String fileName, String absFilePath, String fileType,
			long l, Integer linesOfCode, JsonElement codeJson) {
		super();
		this.fileName = fileName;
		this.absFilePath = absFilePath;
		this.fileType = fileType;
		this.fileSize = l;
		this.linesOfCode = linesOfCode;
		this.codeJson = codeJson;
	}

	/**
	 * 
	 * @return The fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * 
	 * @param fileName
	 *            The fileName
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * 
	 * @return The absFilePath
	 */
	public String getAbsFilePath() {
		return absFilePath;
	}

	/**
	 * 
	 * @param absFilePath
	 *            The absFilePath
	 */
	public void setAbsFilePath(String absFilePath) {
		this.absFilePath = absFilePath;
	}

	/**
	 * 
	 * @return The fileExtension
	 */
	public String getFileExtension() {
		return fileType;
	}

	/**
	 * 
	 * @param fileExtension
	 *            The fileExtension
	 */
	public void setFileExtension(String fileExtension) {
		this.fileType = fileExtension;
	}

	/**
	 * 
	 * @return The fileSize
	 */
	public long getFileSize() {
		return fileSize;
	}

	/**
	 * 
	 * @param fileSize
	 *            The fileSize
	 */
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	/**
	 * 
	 * @return The linesOfCode
	 */
	public Integer getLinesOfCode() {
		return linesOfCode;
	}

	/**
	 * 
	 * @param linesOfCode
	 *            The linesOfCode
	 */
	public void setLinesOfCode(Integer linesOfCode) {
		this.linesOfCode = linesOfCode;
	}

	/**
	 * 
	 * @return The codeJson
	 */
	public JsonElement getCodeJson() {
		return codeJson;
	}

	/**
	 * 
	 * @param codeJson
	 *            The code_json
	 */
	public void setCodeJson(JsonElement codeJson) {
		this.codeJson = codeJson;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(fileName).append(absFilePath)
				.append(fileType).append(fileSize).append(linesOfCode)
				.append(codeJson).toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof ConfigFile) == false) {
			return false;
		}
		ConfigFile rhs = ((ConfigFile) other);
		return new EqualsBuilder().append(fileName, rhs.fileName)
				.append(absFilePath, rhs.absFilePath)
				.append(fileType, rhs.fileType).append(fileSize, rhs.fileSize)
				.append(linesOfCode, rhs.linesOfCode)
				.append(codeJson, rhs.codeJson).isEquals();
	}

}
