
package uic.semit.Project.ProjectData.configurationFilesJSON;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Generated("org.jsonschema2pojo")
public class ConfigFile {

    @Expose
    private String fileName;
    @Expose
    private String absFilePath;
    @Expose
    private String fileExtension;
    @Expose
    private String FileSize;
    @Expose
    private String linesOfCode;
    @SerializedName("config_code")
    @Expose
    private String configCode;

    /**
     * 
     * @return
     *     The fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 
     * @param fileName
     *     The fileName
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 
     * @return
     *     The absFilePath
     */
    public String getAbsFilePath() {
        return absFilePath;
    }

    /**
     * 
     * @param absFilePath
     *     The absFilePath
     */
    public void setAbsFilePath(String absFilePath) {
        this.absFilePath = absFilePath;
    }

    /**
     * 
     * @return
     *     The fileExtension
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * 
     * @param fileExtension
     *     The fileExtension
     */
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    /**
     * 
     * @return
     *     The FileSize
     */
    public String getFileSize() {
        return FileSize;
    }

    /**
     * 
     * @param FileSize
     *     The FileSize
     */
    public void setFileSize(String FileSize) {
        this.FileSize = FileSize;
    }

    /**
     * 
     * @return
     *     The linesOfCode
     */
    public String getLinesOfCode() {
        return linesOfCode;
    }

    /**
     * 
     * @param linesOfCode
     *     The linesOfCode
     */
    public void setLinesOfCode(String linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    /**
     * 
     * @return
     *     The configCode
     */
    public String getConfigCode() {
        return configCode;
    }

    /**
     * 
     * @param configCode
     *     The config_code
     */
    public void setConfigCode(String configCode) {
        this.configCode = configCode;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(fileName).append(absFilePath).append(fileExtension).append(FileSize).append(linesOfCode).append(configCode).toHashCode();
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
        return new EqualsBuilder().append(fileName, rhs.fileName).append(absFilePath, rhs.absFilePath).append(fileExtension, rhs.fileExtension).append(FileSize, rhs.FileSize).append(linesOfCode, rhs.linesOfCode).append(configCode, rhs.configCode).isEquals();
    }

}