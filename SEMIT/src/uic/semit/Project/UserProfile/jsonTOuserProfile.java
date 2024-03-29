package uic.semit.Project.UserProfile;

import uic.semit.Project.ProjectData.ReadJSONfromURL;

import com.google.gson.Gson;

public class jsonTOuserProfile {
	public static UserProfile getUserProfile(String developerName) {

		Gson gson = new Gson();
		UserProfile userprofile = null;
		// http://sourceforge.net/rest/u/[username]/profile
		/*
		 * BufferedReader br = new BufferedReader(new FileReader(
		 * "./userprofile.json"));
		 */
		String userprofileJSON = ReadJSONfromURL
				.getJson("http://sourceforge.net/rest/u/" + developerName
						+ "/profile");
		// convert the json string back to object
		userprofile = gson.fromJson(userprofileJSON, UserProfile.class);

		return userprofile;
	}

	// public static void main(String[] args)
	// {
	//
	// System.err.println(jsonTOuserProfile.getUserProfile("quainter"));
	// }
}
