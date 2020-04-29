package com.tuya.smartai.demo;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tuya.smartai.iot_sdk.DPEvent;

import java.util.Arrays;
import java.util.List;

public class DPEventAdapter extends RecyclerView.Adapter<DPEventViewHolder> {

    private List<DPEvent> mData;

    public DPEventAdapter(List<DPEvent> data) {
        this.mData = data;
    }

    public void setData(List<DPEvent> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DPEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item_layout, null);
        return new DPEventViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DPEventViewHolder holder, int position) {
        holder.render(mData.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).type;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }
}

class DPEventViewHolder extends RecyclerView.ViewHolder {

    String[] type_strs = {"BOOL", "INTEGER", "STRING", "ENUM", "BITMAP", "RAW"};

    TextView mId;
    TextView mType;
    EditText mValue;
    CheckBox mCheck;
    Switch mSwitch;
    Spinner mSpinner;

    DPEventViewHolder(@NonNull View itemView) {
        super(itemView);
        mId = itemView.findViewById(R.id.dp_id);
        mType = itemView.findViewById(R.id.dp_type);
        mValue = itemView.findViewById(R.id.dp_value);
        mCheck = itemView.findViewById(R.id.dp_check);
        mSwitch = itemView.findViewById(R.id.dp_switch);
        mSpinner = itemView.findViewById(R.id.dp_spinner);
    }

    public void render(DPEvent event) {
        mId.setText(event.dpid + "");
        mType.setText(type_strs[event.type]);

        switch (event.type) {
            case DPEvent.Type.PROP_BOOL:
                mValue.setVisibility(View.GONE);
                mSwitch.setVisibility(View.VISIBLE);
                mSpinner.setVisibility(View.GONE);
                break;
            case DPEvent.Type.PROP_VALUE:
                mValue.setInputType(InputType.TYPE_CLASS_PHONE);
            case DPEvent.Type.PROP_STR:
            case DPEvent.Type.PROP_BITMAP:
            case DPEvent.Type.PROP_RAW:
                mValue.setVisibility(View.VISIBLE);
                mSwitch.setVisibility(View.GONE);
                mSpinner.setVisibility(View.GONE);
                break;
            case DPEvent.Type.PROP_ENUM:
                mValue.setVisibility(View.GONE);
                mSwitch.setVisibility(View.GONE);
                mSpinner.setVisibility(View.VISIBLE);
//                mSpinner.setAdapter(new SimpleAdapter(itemView.getContext(), Arrays.asList("YESTERDAY", "TODAY", "TOMORROW"), ));
                break;
        }
    }
}