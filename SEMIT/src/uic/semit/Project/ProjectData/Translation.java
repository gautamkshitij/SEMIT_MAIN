package uic.semit.Project.ProjectData;

import javax.annotation.Generated;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Generated("org.jsonschema2pojo")
public class Translation
{

	private String fullname;
	private String fullpath;
	private long id;
	private String shortname;

	public Translation(String fullname, String fullpath, long id,
			String shortname)
	{

		this.fullname = fullname;
		this.fullpath = fullpath;
		this.id = id;
		this.shortname = shortname;
	}

	/**
	 * 
	 * @return The fullname
	 */
	public String getFullname()
	{
		return fullname;
	}

	/**
	 * 
	 * @param fullname
	 *            The fullname
	 */
	public void setFullname(String fullname)
	{
		this.fullname = fullname;
	}

	public Translation withFullname(String fullname)
	{
		this.fullname = fullname;
		return this;
	}

	/**
	 * 
	 * @return The fullpath
	 */
	public String getFullpath()
	{
		return fullpath;
	}

	/**
	 * 
	 * @param fullpath
	 *            The fullpath
	 */
	public void setFullpath(String fullpath)
	{
		this.fullpath = fullpath;
	}

	public Translation withFullpath(String fullpath)
	{
		this.fullpath = fullpath;
		return this;
	}

	/**
	 * 
	 * @return The id
	 */
	public long getId()
	{
		return id;
	}

	/**
	 * 
	 * @param id
	 *            The id
	 */
	public void setId(long id)
	{
		this.id = id;
	}

	public Translation withId(long id)
	{
		this.id = id;
		return this;
	}

	/**
	 * 
	 * @return The shortname
	 */
	public String getShortname()
	{
		return shortname;
	}

	/**
	 * 
	 * @param shortname
	 *            The shortname
	 */
	public void setShortname(String shortname)
	{
		this.shortname = shortname;
	}

	public Translation withShortname(String shortname)
	{
		this.shortname = shortname;
		return this;
	}

	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder().append(fullname).append(fullpath)
				.append(id).append(shortname).toHashCode();
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == this) { return true; }
		if ((other instanceof Translation) == false) { return false; }
		Translation rhs = ((Translation) other);
		return new EqualsBuilder().append(fullname, rhs.fullname)
				.append(fullpath, rhs.fullpath).append(id, rhs.id)
				.append(shortname, rhs.shortname)

				.isEquals();
	}

}
