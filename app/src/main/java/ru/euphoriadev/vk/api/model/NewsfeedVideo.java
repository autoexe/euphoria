package ru.euphoriadev.vk.api.model;

import android.content.Context;
import android.os.Parcelable;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ru.euphoriadev.vk.adapter.NewsVideosAdapter;
import ru.euphoriadev.vk.async.ThreadExecutor;
import ru.euphoriadev.vk.util.Item;

/**
 * Created by autoexec on 08.02.2017.
 */

public class NewsfeedVideo{


    private static Item item;
    private static NewsVideosAdapter adapter;

    public static NewsfeedVideo parse(final JSONObject response) throws JSONException{

        if(response != null) {
            final boolean[] allNewsDownloaded = {false};
            if (!allNewsDownloaded[0]) {
                ThreadExecutor.execute(new Runnable() {

                    public String start_from = "";
                    ArrayList itemsAll = new ArrayList();
                    ArrayList itemsDownloadable = new ArrayList();

                    @Override
                    public void run() {
                        while (!allNewsDownloaded[0]){
                            JSONObject jsonObject2 = null;
                            JSONObject jsonObject = new JSONObject();
                            JSONArray jsonArray = new JSONArray();
                            try {
                                jsonObject.put("items", (Object) jsonArray);
                                for (int i = 0; i < response.getJSONArray("items").length(); i++) {
                                    JSONArray jsonArray2 = response.getJSONArray("items").getJSONObject(i).getJSONObject("video").getJSONArray("items");
                                    for (int j = 0; j < jsonArray2.length(); j++) {
                                        jsonArray.put(jsonArray2.get(j));

                                    }
                                }
                                if (response.has("next_from")) {
                                    start_from = response.getString("next_from");
                                    allNewsDownloaded[0] = true;
                                }
                                jsonObject.put("count", jsonArray.length());
                                JSONArray jsonArray3 = jsonObject.getJSONArray("items");
                                for (int k = 0; k < jsonArray3.length(); k++){
                                    item = new Item();
                                    jsonObject2 = jsonArray3.getJSONObject(k);
                                    item.setOwner_id(jsonObject2.getInt("owner_id"));
                                    item.setId(jsonObject2.getInt("id"));
                                    if (!jsonObject2.has("photo_320")) {
                                        break;
                                    }
                                    item.setPhoto_320(jsonObject2.getString("photo_320"));
                                    item.setTitle(jsonObject2.getString("title"));
                                    item.setDuration(jsonObject2.getInt("duration"));
                                    item.setViews(jsonObject2.getInt("views"));
                                    item.setLikes(jsonObject2.getJSONObject("likes").getInt("count"));
                                    item.setAccess_key(jsonObject2.getString("access_key"));
                                    if (jsonObject2.has("platform")) {
                                        item.setPlatform(jsonObject2.getString("platform"));
                                        itemsAll.add(item);
                                    }
                                    if (jsonObject2.has("photo_130")) {
                                        item.setPhoto_130(jsonObject2.getString("photo_130"));
                                        itemsDownloadable.add(item);
                                    }
                                    adapter = new NewsVideosAdapter(this, itemsDownloadable);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        }
        NewsfeedVideo newsVideo = new NewsfeedVideo();


        return newsVideo;
    }
}
