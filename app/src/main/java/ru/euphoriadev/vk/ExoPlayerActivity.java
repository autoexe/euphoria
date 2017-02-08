package ru.euphoriadev.vk;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaDataSource;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

/**
 * Created by autoexec on 08.02.2017.
 */

public class ExoPlayerActivity extends Activity implements View.OnClickListener, ExoPlayer.EventListener,
        PlaybackControlView.VisibilityListener {

    private DataSource.Factory mediaDataSourceFactory;

    private Handler mainHandler;
    private LinearLayout debugRootView;
    private TextView debugTextView;
    private Button retryButton;
    private SimpleExoPlayerView simpleExoPlayerView;

    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;

    public static final String URI_LIST_EXTRA = "uri_list";

    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private boolean playerNeedsSource;

    public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shouldAutoPlay = true;
        clearResumePosition();
        mainHandler = new Handler();

        setContentView(R.layout.exo_player_activity);
        View rootView = findViewById(R.id.root);
        rootView.setOnClickListener(this);
        debugRootView = (LinearLayout) findViewById(R.id.controls_root);
        debugTextView = (TextView) findViewById(R.id.debug_text_view);
        retryButton = (Button) findViewById(R.id.retry_button);
        retryButton.setOnClickListener(this);

        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
        simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.requestFocus();

        initializePlayer();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void initializePlayer(){
        Intent intent = getIntent();
        if (player == null){
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl());
            player.addListener(this);

            simpleExoPlayerView.setPlayer(player);
            player.setPlayWhenReady(shouldAutoPlay);
            playerNeedsSource = true;
        }
        if (playerNeedsSource){
            String uriString = intent.getStringExtra(URI_LIST_EXTRA);
            Uri mp4VideoUri = Uri.parse(uriString);
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Euphoria"), bandwidthMeter);
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            MediaSource videoSource = new ExtractorMediaSource(mp4VideoUri, dataSourceFactory, extractorsFactory, null, null);
            player.prepare(videoSource);
            playerNeedsSource = false;
        }
    }


    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onVisibilityChange(int visibility) {

    }
}
