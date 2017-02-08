package ru.euphoriadev.vk.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Igor on 16.07.15.
 */
public class VKUser implements Serializable {

    /** Empty user object */
    public static final VKUser EMPTY = new VKUser() {
        @Override
        public String toString() {
            return "";
        }
    };

    public static final String FIELDS_DEFAULT = "photo_50, photo_100, photo_200, status, screen_name, online, online_mobile, ";
    /**
     * User ID.
     */
    public int user_id;
    /**
     * First name of user.
     */
    public String first_name = "DELETED";
    /**
     * Last name of user.
     */
    public String last_name = "";
    /**
     * Status of User
     */
    public String status = "";
    /**
     * User page's screen name (subdomain)
     */
    public String screen_name;
    /**
     * Information whether the user is online.
     */
    public boolean online;

    /**
     * If user utilizes a mobile application or site mobile version, it returns online_mobile as additional.
     */
    public boolean online_mobile;

    /**
     * ID of mobile application, if user is online
     */
    public int online_app;

    /**
     * URL of default square photo of the user with 50 pixels in width.
     */
    public String photo_50 = "http://vk.com/images/camera_c.gif";

    /**
     * URL of default square photo of the user with 100 pixels in width.
     */
    public String photo_100 = "http://vk.com/images/camera_b.gif";

    /**
     * URL of default square photo of the user with 200 pixels in width.
     */
    public String photo_200 = "http://vk.com/images/camera_a.gif";
    /**
     * A Cached full name of user
     *
     * @see #toString()
     */
    private String fullName;

    public static VKUser parse(JSONObject source) {
        VKUser user = new VKUser();
        user.user_id = source.optInt("id");
        user.first_name = source.optString("first_name", "DELETED");
        user.last_name = source.optString("last_name");
        user.photo_50 = source.optString("photo_50");
        user.photo_50 = source.optString("photo_100");
        user.photo_200 = source.optString("photo_200");
        user.screen_name = source.optString("screen_name");
        user.online = source.optInt("online") == 1;
        user.status = source.optString("status");
        user.online_mobile = source.optInt("online_mobile") == 1;
        if (user.online_mobile) {
            user.online_app = source.optInt("online_app");
        }
//        Log.w("VKApi", String.format("name: %s, online: %s, online mobile: %s, online app %s", user.toString(), user.online, user.online_mobile, user.online_app));

        return user;
    }

    public static ArrayList<VKUser> parseUsers(JSONArray array) {
        ArrayList<VKUser> vkUsers = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            VKUser user = VKUser.parse(array.optJSONObject(i));
            vkUsers.add(user);
        }
        return vkUsers;
    }

    @Override
    public String toString() {
        if (fullName == null) {
            fullName = first_name.concat(" ").concat(last_name);
        }
        return fullName;
    }
}
