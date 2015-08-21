
package uic.semit.Project.ProjectData.configurationFilesJSON;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Generated("org.jsonschema2pojo")
public class ConfigFiles {

    @Expose
    private List<ConfigurationFile> configurationFiles = new ArrayList<ConfigurationFile>();

    /**
     * 
     * @return
     *     The files
     */
    public List<ConfigurationFile> getFiles() {
        return configurationFiles;
    }

    /**
     * 
     * @param configurationFiles
     *     The files
     */
    public void setFiles(List<ConfigurationFile> configurationFiles) {
        this.configurationFiles = configurationFiles;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(configurationFiles).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ConfigFiles) == false) {
            return false;
        }
        ConfigFiles rhs = ((ConfigFiles) other);
        return new EqualsBuilder().append(configurationFiles, rhs.configurationFiles).isEquals();
    }

}
