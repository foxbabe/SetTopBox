package com.savor.tvlibrary;

import android.util.Log;
import android.view.SurfaceHolder;

import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TVChannelParams;
import com.droidlogic.app.tv.TvControlManager;

/**
 * Created by zhang.haiqiang on 2017/9/26.
 */

public class JackTVOperator implements ITVOperator {
    @Override
    public void setDisplay(SurfaceHolder holder) {
        TvControlManager.getInstance().StartTv();
    }

    @Override
    public void switchATVChannel(int channelNum) {

    }

    @Override
    public void autoTuning(final AutoTurningCallback turningCallback) {
        final String TAG = "autoTurning";
        TvControlManager.getInstance().setScannerListener(new TvControlManager.ScannerEventListener() {
            @Override
            public void onEvent(TvControlManager.ScannerEvent event) {
                ChannelInfo channel = null;
                String name = null;

//                if (!isSearching())
//                    return;

                switch (event.type) {
                    case TvControlManager.EVENT_SCAN_PROGRESS:
                        int isNewProgram = 0;
                        Log.d(TAG, "onEvent:"+event.precent + "%\tfreq[" + event.freq + "] lock[" + event.lock + "] strength[" + event.strength + "] quality[" + event.quality + "]");

//                        if ((event.mode == TVChannelParams.MODE_ANALOG) && (event.lock == 0x11)) { //trick here
//                            isNewProgram = 1;
//                            Log.d(TAG, "Resume Scanning");
//                            if ((mTvControlManager.AtvDtvGetScanStatus() & TvControlManager.ATV_DTV_SCAN_STATUS_PAUSED)
//                                    == TvControlManager.ATV_DTV_SCAN_STATUS_PAUSED)
//                                resumeSearch();
//                        } else if ((event.mode != TVChannelParams.MODE_ANALOG) && (event.programName.length() != 0)) {
//                            try {
//                                name = TVMultilingualText.getText(event.programName);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                            isNewProgram = 2;
//                        }
//
//                        if (isNewProgram == 1) {
//                            channelNumber++;
//                            Log.d(TAG, "New ATV Program");
//                        } else if (isNewProgram == 2) {
//                            if (event.srvType == 1)
//                                channelNumber++;
//                            else if (event.srvType == 2)
//                                radioNumber++;
//                            Log.d(TAG, "New DTV Program : [" + name + "] type[" + event.srvType + "]");
//                        }

                        turningCallback.onProgressUpdate(event.precent);
//                        setProgress(event.precent);
//                        if (optionTag == OPTION_MANUAL_SEARCH)
//                            setManualSearchInfo(event);
//                        else
//                            setAutoSearchFrequency(event);

                        if ((event.mode == TVChannelParams.MODE_ANALOG) /*&& (optionTag == OPTION_MANUAL_SEARCH)*/
                                && event.precent == 100) {
//                            stopSearch();
                            TvControlManager.getInstance().OpenDevForScan(DroidLogicTvUtils.CLOSE_DEV_FOR_SCAN);
                            TvControlManager.getInstance().DtvStopScan();
                        }
                        break;

                    case TvControlManager.EVENT_STORE_BEGIN:
                        Log.d(TAG, "onEvent:Store begin");
                        break;

                    case TvControlManager.EVENT_STORE_END:
                        Log.d(TAG, "onEvent:Store end");
//                        String prompt = mResources.getString(R.string.searched);
//                        if (channelNumber != 0) {
//                            prompt += " " + channelNumber + " " + mResources.getString(R.string.tv_channel);
//                            if (radioNumber != 0) {
//                                prompt += ",";
//                            }
//                        }
//                        if (radioNumber != 0) {
//                            prompt += " " + radioNumber + " " + mResources.getString(R.string.radio_channel);
//                        }
//                        showToast(prompt);
                        break;

                    case TvControlManager.EVENT_SCAN_END:
                        Log.d(TAG, "onEvent:Scan end");
//                        stopSearch();
                        TvControlManager.getInstance().OpenDevForScan(DroidLogicTvUtils.CLOSE_DEV_FOR_SCAN);
                        TvControlManager.getInstance().DtvStopScan();
                        break;

                    case TvControlManager.EVENT_SCAN_EXIT:
                        Log.d(TAG, "onEvent:Scan exit.");
//                        SystemControlManager scm = new SystemControlManager(mContext);
//                        scm.setProperty("tv.channels.count", ""+(channelNumber+radioNumber));
//                        isSearching = SEARCH_STOPPED;
//                        ((TvSettingsActivity) mContext).finish();
//                        if (channelNumber == 0 && radioNumber == 0) {
//                            showToast(mResources.getString(R.string.searched) + " 0 " + mResources.getString(R.string.channel));
//                        }
                        break;
                    default:
                        break;
                }
            }
        });

        TvControlManager.getInstance().OpenDevForScan(DroidLogicTvUtils.OPEN_DEV_FOR_SCAN_ATV);
        TvControlManager.getInstance().AtvAutoScan(TvControlManager.ATV_VIDEO_STD_PAL, TvControlManager.ATV_AUDIO_STD_DK);
    }

    @Override
    public void interruptTuning() {
        TvControlManager.getInstance().OpenDevForScan(DroidLogicTvUtils.CLOSE_DEV_FOR_SCAN);
        TvControlManager.getInstance().DtvStopScan();
    }

    @Override
    public AtvChannel[] getAtvChannels() {
//        TvControlManager.getInstance().ATVGetChanInfo()
        return new AtvChannel[0];
    }

    @Override
    public void setAtvChannels(AtvChannel[] channels) {

    }

    @Override
    public TVSignal getCurrentSignalSource() {
        TVSignal tvSignal = TVSignal.ATV;
        int source = TvControlManager.getInstance().GetCurrentSourceInput();
        if (source == TvControlManager.SourceInput.TV.toInt()) {
            tvSignal = TVSignal.ATV;
        } else if (source == TvControlManager.SourceInput.AV1.toInt()) {
            tvSignal = TVSignal.AVI;
        } else if (source == TvControlManager.SourceInput.HDMI1.toInt()) {
            tvSignal = TVSignal.HDMI;
        }
        return tvSignal;
    }

    @Override
    public void setSignalSource(TVSignal signal) {
        TvControlManager.SourceInput source = TvControlManager.SourceInput.TV;
        switch (signal) {
            case ATV:
                source = TvControlManager.SourceInput.TV;
                break;
            case AVI:
                source = TvControlManager.SourceInput.AV1;
                break;
            case HDMI:
                source = TvControlManager.SourceInput.HDMI3;
                break;
        }
        TvControlManager.getInstance().SetSourceInput(source);
    }

    @Override
    public void exitTv() {
        TvControlManager.getInstance().StopTv();
        TvControlManager.getInstance().release();
    }
}
