package com.savor.ads.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;


/**
 * 设置界面左侧列表适配器
 */
public class SetListAdapter extends BaseAdapter {
	private int mTo;
    private List<String> mData;
    private int mResource;
    private LayoutInflater mInflater;
    
	
	public SetListAdapter(Context context, List<String> data,
						  int resource, int to) {
        mData = data;
        mResource = resource;
        mTo = to;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
	 
	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//LogUtils.i(TV.TAG, "getView position:"+position);
		View v = convertView;
        if(v == null) {
            v = mInflater.inflate(mResource, parent, false);
        }
        bindView(position, v);
		return v;
	}

	private void bindView(int position, View view) {
		final String channel = mData.get(position);
		if(channel == null) return;
		TextView text = (TextView) view.findViewById(mTo);
		text.setText(channel);
	}
	
}
