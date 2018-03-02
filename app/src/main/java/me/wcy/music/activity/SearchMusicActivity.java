package me.wcy.music.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.wcy.music.R;
import me.wcy.music.adapter.OnMoreClickListener;
import me.wcy.music.adapter.SearchMusicAdapter;
import me.wcy.music.enums.LoadStateEnum;
import me.wcy.music.executor.PlaySearchedMusic;
import me.wcy.music.executor.ShareOnlineMusic;
import me.wcy.music.http.HttpCallback;
import me.wcy.music.http.HttpClient;
import me.wcy.music.model.Music;
import me.wcy.music.model.SearchMusic;
import me.wcy.music.service.AudioPlayer;
import me.wcy.music.utils.FileUtils;
import me.wcy.music.utils.ToastUtils;
import me.wcy.music.utils.ViewUtils;
import me.wcy.music.utils.binding.Bind;

public class SearchMusicActivity extends BaseActivity implements SearchView.OnQueryTextListener
        , AdapterView.OnItemClickListener, OnMoreClickListener {
    @Bind(R.id.lv_search_music_list)
    private ListView lvSearchMusic;
    @Bind(R.id.ll_loading)
    private LinearLayout llLoading;
    @Bind(R.id.ll_load_fail)
    private LinearLayout llLoadFail;
    private List<SearchMusic.Song> searchMusicList = new ArrayList<>();
    private SearchMusicAdapter mAdapter = new SearchMusicAdapter(searchMusicList);
    private String searchString;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_music);
        searchString = null;
        Intent intent = getIntent();
        if (null != intent) {
            Uri uri = intent.getData();
            if (uri == null) {
                return;
            }
            searchString = uri.getQueryParameter("action");
//            String musicName = ""; //歌手－歌名
//            if (wakeupStr.contains("播放") && wakeupStr.contains("的音乐")) {//播放＊＊＊的音乐
//                musicName = wakeupStr.substring(wakeupStr.indexOf("播放") + 2 , wakeupStr.indexOf("的音乐"));
//            } else if (wakeupStr.contains("播放") && wakeupStr.contains("的歌曲")) {//播放＊＊的＊＊   播放许巍的歌曲
//                musicName = wakeupStr.substring(wakeupStr.indexOf("播放") + 2 , wakeupStr.indexOf("的歌曲"));
//            } else if (wakeupStr.contains("播放") && wakeupStr.contains("的")) {//播放＊＊的＊＊   播放许巍的故乡
//                musicName = wakeupStr.substring(wakeupStr.indexOf("播放") + 2 , wakeupStr.indexOf("的"));
//                musicName += "-" + wakeupStr.substring(wakeupStr.indexOf("的") + 1);
//            }
            Log.i("searchString", "searchString=" + searchString);
        }
    }

    @Override
    protected void onServiceBound() {
        lvSearchMusic.setAdapter(mAdapter);
        TextView tvLoadFail = llLoadFail.findViewById(R.id.tv_load_fail_text);
        tvLoadFail.setText(R.string.search_empty);

        lvSearchMusic.setOnItemClickListener(this);
        mAdapter.setOnMoreClickListener(this);
    }

    @Override
    protected int getDarkTheme() {
        return R.style.AppThemeDark_Search;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_music, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.onActionViewExpanded();
        searchView.setQueryHint(getString(R.string.search_tips));
        searchView.setOnQueryTextListener(this);
        searchView.setSubmitButtonEnabled(true);
        try {
            Field field = searchView.getClass().getDeclaredField("mGoButton");
            field.setAccessible(true);
            ImageView mGoButton = (ImageView) field.get(searchView);
            mGoButton.setImageResource(R.drawable.ic_menu_search);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (searchString != null) {
            if (searchString.contains("-")) {
                String musicName = searchString.substring(searchString.indexOf("-") + 1);
                searchView.setQuery(musicName, true);
            } else  {
                searchView.setQuery(searchString, true);
            }

        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOADING);
        searchMusic(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private void searchMusic(String keyword) {
//        HttpClientWangyi.searchMusic(keyword, new HttpCallback<SearchMusic>() {
//            @Override
//            public void onSuccess(SearchMusic searchMusic) {
//
//            }
//
//            @Override
//            public void onFail(Exception e) {
//
//            }
//        });

        HttpClient.searchMusic(keyword, new HttpCallback<SearchMusic>() {
            @Override
            public void onSuccess(SearchMusic response) {
                if (response == null || response.getSong() == null) {
                    ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOAD_FAIL);
                    return;
                }
                ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOAD_SUCCESS);
                searchMusicList.clear();
                List<SearchMusic.Song> searchlist = response.getSong();
                if (searchString != null) {
                    if (searchString.contains("-")) {
                        String musicArtist = searchString.substring(0,searchString.indexOf("-"));
                        Log.i("searchlist", "musicArtist=" + musicArtist);
                        int size = searchlist.size();
                        for (int i = 0; i < size; i++) {
                            SearchMusic.Song song = searchlist.get(i);
                            String name = song.getArtistname();
                            Log.i("searchlist", "name=" + name);
                            if (name.contains("musicArtist") || musicArtist.contains(name)) {
                                searchlist.clear();
                                searchlist.add(song);
                                break;
                            }
                        }
                    } else {
                        int size = searchlist.size();
                        for (int i = 0; i < size; i++) {
                            SearchMusic.Song song = searchlist.get(i);
                            String name = song.getSongname();
                            Log.i("searchlist", "name=" + name);
                            if (name.contains(searchString) || searchString.contains(name)) {
                                searchlist.clear();
                                searchlist.add(song);
                                break;
                            }
                        }
                    }

                }
                searchMusicList.addAll(searchlist);
                mAdapter.notifyDataSetChanged();
                lvSearchMusic.requestFocus();
                handler.post(() -> lvSearchMusic.setSelection(0));
                if (searchString != null) {
//                    listView.performItemClick(listView.getAdapter().getView(click_position, null, null), click_position, listView.(click_position));
                    lvSearchMusic.performItemClick(lvSearchMusic.getChildAt(0), 0, lvSearchMusic.getItemIdAtPosition(0));
//                    lvSearchMusic.performItemClick(lvSearchMusic.getAdapter().getView(0,null,null), 0, lvSearchMusic.getItemIdAtPosition(0));
//                    finish();
                }
            }

            @Override
            public void onFail(Exception e) {
                ViewUtils.changeViewState(lvSearchMusic, llLoading, llLoadFail, LoadStateEnum.LOAD_FAIL);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        new PlaySearchedMusic(this, searchMusicList.get(position)) {
            @Override
            public void onPrepare() {
                showProgress();
            }

            @Override
            public void onExecuteSuccess(Music music) {
                cancelProgress();
                AudioPlayer.get().addAndPlay(music);
                ToastUtils.show("已添加到播放列表");
            }

            @Override
            public void onExecuteFail(Exception e) {
                cancelProgress();
                ToastUtils.show(R.string.unable_to_play);
            }
        }.execute();
    }

    @Override
    public void onMoreClick(int position) {
        final SearchMusic.Song song = searchMusicList.get(position);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(song.getSongname());
        String path = FileUtils.getMusicDir() + FileUtils.getMp3FileName(song.getArtistname(), song.getSongname());
        File file = new File(path);
        int itemsId = file.exists() ? R.array.search_music_dialog_no_download : R.array.search_music_dialog;
        dialog.setItems(itemsId, (dialog1, which) -> {
            switch (which) {
                case 0:// 分享
                    share(song);
                    break;
                case 1:// 下载
//                    download(song);
                    break;
            }
        });
        dialog.show();
    }

    private void share(SearchMusic.Song song) {
        new ShareOnlineMusic(this, song.getSongname(), song.getSongid()) {
            @Override
            public void onPrepare() {
                showProgress();
            }

            @Override
            public void onExecuteSuccess(Void aVoid) {
                cancelProgress();
            }

            @Override
            public void onExecuteFail(Exception e) {
                cancelProgress();
            }
        }.execute();
    }

//    private void download(final SearchMusic.Song song) {
//        new DownloadSearchedMusic(this, song) {
//            @Override
//            public void onPrepare() {
//                showProgress();
//            }
//
//            @Override
//            public void onExecuteSuccess(Void aVoid) {
//                cancelProgress();
//                ToastUtils.show(getString(R.string.now_download, song.getSongname()));
//            }
//
//            @Override
//            public void onExecuteFail(Exception e) {
//                cancelProgress();
//                ToastUtils.show(R.string.unable_to_download);
//            }
//        }.execute();
//    }
}
