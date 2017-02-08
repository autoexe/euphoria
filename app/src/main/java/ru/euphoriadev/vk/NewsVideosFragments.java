package ru.euphoriadev.vk;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import ru.euphoriadev.vk.api.Api;
import ru.euphoriadev.vk.api.model.VKVideo;
import ru.euphoriadev.vk.async.ThreadExecutor;
import ru.euphoriadev.vk.common.AppLoader;
import ru.euphoriadev.vk.util.Item;

/**
 * Created by autoexec on 08.02.2017.
 */

public class NewsVideosFragments extends android.support.v4.app.Fragment {
    public static final String TAG = "NewsVideosFragments";

    ListView lvNewsVideos;
    AppLoader mAppLoader;
    Api vkApi;
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.news_videos_fragment, container, false);
        lvNewsVideos = (ListView) rootView.findViewById(R.id.navNewsVideos);
        mAppLoader = AppLoader.getLoader();
        vkApi = Api.get();
        requestNewsVideos();

        return rootView;
    }

    private void requestNewsVideos() {
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final ArrayList<Item> newsfeedVideos = vkApi.getNewsfeedVideo();
                }catch (Exception e){
                    Log.e(TAG, "Error get videos", e);
                }
            }
        });
    }
}
