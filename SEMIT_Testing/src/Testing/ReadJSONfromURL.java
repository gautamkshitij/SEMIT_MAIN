package Testing;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class ReadJSONfromURL
{

	@SuppressWarnings("deprecation")
	public static String getJson(String ProjectURL)
	{
		String json_string = "";
		try
		{

			@SuppressWarnings("deprecation")
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet getRequest = new HttpGet(ProjectURL);
			getRequest.addHeader("accept", "application/json");

			HttpResponse response = httpClient.execute(getRequest);

			if (response.getStatusLine().getStatusCode() != 200)
			{
				System.err.println(response.getStatusLine().getStatusCode()
						+ " - " + ProjectURL);
				// throw new RuntimeException("Failed : HTTP error code : "+
				// response.getStatusLine().getStatusCode());
			}
			else
			{
				json_string = EntityUtils.toString(response.getEntity());
				return json_string;
			}
			httpClient.getConnectionManager().shutdown();

		}
		catch (Exception e)
		{

			System.err.println(ProjectURL);

		}
		return json_string;

	}
}
