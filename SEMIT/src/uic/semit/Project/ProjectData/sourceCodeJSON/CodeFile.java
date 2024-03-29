package uic.semit.Project.ProjectData.sourceCodeJSON;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Generated("org.jsonschema2pojo")
public class CodeFile {

	@Expose
	private String repository;
	@Expose
	private List<SourceCode> sourceCode = new ArrayList<SourceCode>();

	public CodeFile(String repository, List<SourceCode> sourceCode) {
		super();
		this.repository = repository;
		this.sourceCode = sourceCode;
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
	public List<SourceCode> getSourceCode() {
		return sourceCode;
	}

	/**
	 * 
	 * @param sourceCode
	 *            The sourceCode
	 */
	public void setSourceCode(List<SourceCode> sourceCode) {
		this.sourceCode = sourceCode;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(repository).append(sourceCode)
				.toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof CodeFile) == false) {
			return false;
		}
		CodeFile rhs = ((CodeFile) other);
		return new EqualsBuilder().append(repository, rhs.repository)
				.append(sourceCode, rhs.sourceCode).isEquals();
	}

}
