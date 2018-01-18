package com.savor.ads.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.savor.ads.R;
import com.savor.ads.adapter.PlaylistAdapter;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.core.Session;
import com.savor.ads.utils.GlobalValues;

import java.util.ArrayList;

/**
 * Created by zhanghq on 2016/12/12.
 */

public class PlayListDialog extends Dialog {

    private Callback mCallback;

    private TextView mTitleTv;
    private GridView mPlaylistGv;

    private PlaylistAdapter mPlaylistAdapter;

    public PlayListDialog(Context context, Callback callback) {
        super(context, R.style.box_info_dialog_theme);
        mCallback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_play_list);
        setDialogAttributes();

        mTitleTv = (TextView) findViewById(R.id.tv_title);
        mPlaylistGv = (GridView) findViewById(R.id.gv_play_list);

        mPlaylistAdapter = new PlaylistAdapter(getContext(), null);
        mPlaylistGv.setAdapter(mPlaylistAdapter);
        mPlaylistGv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mCallback != null) {
                    mCallback.onMediaItemSelect(position);
                    PlayListDialog.this.dismiss();
                }
            }
        });
    }


    private void setDialogAttributes() {
        Window window = getWindow(); // 得到对话框
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.width = WindowManager.LayoutParams.MATCH_PARENT;
        wl.height = WindowManager.LayoutParams.MATCH_PARENT;
        wl.gravity = Gravity.CENTER;
        window.setAttributes(wl);
    }

    public void showPlaylist(ArrayList<MediaLibBean> playlist) {
        super.show();

        String adsPeriod = Session.get(getContext()).getAdsPeriod();
        String proPeriod = Session.get(getContext()).getProPeriod();
        if (playlist != null) {
            mTitleTv.setText(String.format("播放列表    共%s个内容    广告版本：%s    节目版本：%s", playlist.size(), adsPeriod, proPeriod));
            mPlaylistAdapter.setPlaylist(playlist);

        }
    }

    public interface Callback {
        void onMediaItemSelect(int index);
    }
}
