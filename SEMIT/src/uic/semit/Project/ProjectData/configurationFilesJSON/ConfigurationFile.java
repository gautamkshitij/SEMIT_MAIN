package uic.semit.Project.ProjectData.configurationFilesJSON;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Generated("org.jsonschema2pojo")
public class ConfigurationFile {

	@Expose
	private String repository;
	@Expose
	private List<ConfigFile> configCode = new ArrayList<ConfigFile>();

	public ConfigurationFile(String repository, List<ConfigFile> configCode) {
		super();
		this.repository = repository;
		this.configCode = configCode;
	}

	/**
	 * 
	 * @return The repository
	 */
	public String getRepository() {
		return repository;
	}

	/**
	 * 
	 * @param repository
	 *            The repository
	 */
	public void setRepository(String repository) {
		this.repository = repository;
	}

	/**
	 * 
	 * @return The sourceCode
	 */
	public List<ConfigFile> getSourceCode() {
		return configCode;
	}

	/**
	 * 
	 * @param sourceCode
	 *            The sourceCode
	 */
	public void setSourceCode(List<ConfigFile> sourceCode) {
		this.configCode = sourceCode;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(repository).append(configCode)
				.toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof ConfigurationFile) == false) {
			return false;
		}
		ConfigurationFile rhs = ((ConfigurationFile) other);
		return new EqualsBuilder().append(repository, rhs.repository)
				.append(configCode, rhs.configCode).isEquals();
	}

}
