package com.savor.ads.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.savor.ads.R;
import com.savor.ads.SavorApplication;
import com.savor.ads.bean.MiniProgramProjection;
import com.savor.ads.customview.LuckyMonkeyPanelView;
import com.savor.ads.utils.GlideImageLoader;
import com.savor.ads.utils.ShowMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MonkeyGameActivity extends BaseActivity {

    private Context context;
    private LuckyMonkeyPanelView lucky_panel;
    private Button btn_action;
    private LinearLayout winningPrizeLayout;
    private ImageView winningWeixinHeadIV;
    private TextView winningTextTV;
    private List<MiniProgramProjection> avatarList = new ArrayList<>();
    boolean isGameOver = true;
    private MiniProgramProjection miniProgramProjection;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

                 Message message = msg;
                 String avatarurl = (String) message.obj;
                 int action = message.what;

                  lucky_panel.setImageViewSrc(action,avatarurl);


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monkey_game);
        context = this;
        getViews();
        initContent();
        handler.removeCallbacks(exitRunnable);
        handler.postDelayed(exitRunnable,1000*60*2);
    }

    private void getViews(){
        lucky_panel = (LuckyMonkeyPanelView) findViewById(R.id.lucky_panel);

        btn_action = (Button) findViewById(R.id.btn_action);
        winningPrizeLayout = (LinearLayout) findViewById(R.id.winning_prize_layout);
        winningWeixinHeadIV = (ImageView) findViewById(R.id.winning_weixin_head);
        winningTextTV = (TextView) findViewById(R.id.winning_text);
    }

    private void initContent(){
        ((SavorApplication) getApplication()).hideMiniProgramQrCodeWindow();
        Intent intent = getIntent();
        miniProgramProjection = (MiniProgramProjection)intent.getSerializableExtra("miniProgramProjection");
        if (miniProgramProjection!=null&&!TextUtils.isEmpty(miniProgramProjection.getGamecode())){

//            GlideImageLoader.loadImage(context,miniProgramProjection.getGamecode(),winningWeixinHeadIV);
            Glide.with(context)
                    .load(miniProgramProjection.getGamecode())
                    .dontAnimate()
                    .placeholder(winningWeixinHeadIV.getDrawable())
                    .into(winningWeixinHeadIV);
        }
        if (miniProgramProjection!=null&&!TextUtils.isEmpty(miniProgramProjection.getAvatarurl())){
            avatarList.add(miniProgramProjection);
            Message message = new Message();

            message.obj = miniProgramProjection.getAvatarurl();
            message.what = avatarList.size();
            handler.sendMessage(message);
        }

        lucky_panel.setWinningListener(new WinningListener() {
            @Override
            public void setWinningWeixinHead(final Drawable drawable) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        winningPrizeLayout.setVisibility(View.VISIBLE);
                        winningWeixinHeadIV.setImageDrawable(drawable);
                        isGameOver = true;
                        winningTextTV.setVisibility(View.GONE);
                        winningTextTV.setText("哈哈，您中奖了，干了吧");
                    }
                });

            }
        });
    }


    public void addWeixinAvatarToGame(MiniProgramProjection programProjection){
        handler.removeCallbacks(exitRunnable);
        handler.postDelayed(exitRunnable,1000*60*2);
        if (programProjection!=null&&!TextUtils.isEmpty(programProjection.getAvatarurl())){
            boolean isAdd = false;
            if (avatarList!=null&&avatarList.size()>0){
                if (avatarList.size()>=9){
                    ShowMessage.showToast(this,"已经满员了哦亲");
                    return;
                }
                for (MiniProgramProjection projection:avatarList){
                    if (projection.getOpenid().equals(programProjection.getOpenid())){
                        isAdd =true;
                    }
                }
            }
            if (!isAdd){
                avatarList.add(programProjection);
                Message message = new Message();
                message.obj = programProjection.getAvatarurl();
                message.what = avatarList.size();
                handler.sendMessage(message);
            }

        }

    }


    public void startGame(){
        if (isGameOver){
            isGameOver = false;
            handler.removeCallbacks(exitRunnable);
            handler.postDelayed(exitRunnable,1000*60*2);
            lucky_panel.startGame();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int stayIndex = new Random().nextInt(avatarList.size());
                    Log.e("LuckyMonkeyPanelView", "====stay===" + stayIndex);
                    lucky_panel.tryToStop(stayIndex);
                }
            },1000*10);
        }

    }

    private Runnable exitRunnable = new Runnable() {
        @Override
        public void run() {
            exitGame();
        }
    };


    public void exitGame(){
        finish();
    }

    public interface WinningListener{
        void setWinningWeixinHead(Drawable drawable);
    }
}
