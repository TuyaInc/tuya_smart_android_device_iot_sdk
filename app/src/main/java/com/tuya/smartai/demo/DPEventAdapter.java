package com.tuya.smartai.demo;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tuya.smartai.iot_sdk.DPEvent;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class DPEventAdapter extends RecyclerView.Adapter<DPEventViewHolder> {

    private List<DPEvent> mData;
    private SparseBooleanArray mCheckMap;

    DPEventAdapter(List<DPEvent> data) {
        this.mData = data;
        mCheckMap = new SparseBooleanArray();
    }

    public void setData(List<DPEvent> data) {
        this.mData = data;
        mCheckMap = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    public void updateEvent(DPEvent event) {
        DPEvent aim = mData.stream().filter(e -> e.dpid == event.dpid).findFirst().orElse(null);
        if (aim != null) {
            aim.value = event.value;
            notifyItemChanged(mData.indexOf(aim));
        }
    }

    List<DPEvent> getData() {
        return mData;
    }

    List<DPEvent> getCheckedList() {
        return mData.stream()
                .filter(event -> mCheckMap.get(mData.indexOf(event)))
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public DPEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item_layout, null);
        return new DPEventViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DPEventViewHolder holder, int position) {
        DPEvent event = mData.get(position);
        holder.render(event);
        holder.mCheck.setChecked(mCheckMap.get(position));
        holder.mCheck.setOnCheckedChangeListener((buttonView, isChecked) -> mCheckMap.put(position, isChecked));
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

    private String[] type_strs = {"BOOL", "INTEGER", "STRING", "ENUM", "BITMAP", "RAW"};

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

    void render(DPEvent event) {
        mId.setText(event.dpid + "");

        mType.setText(type_strs[event.type]);

        mValue.setInputType(InputType.TYPE_CLASS_TEXT);
        switch (event.type) {
            case DPEvent.Type.PROP_BOOL:
                mValue.setVisibility(View.GONE);
                mSwitch.setVisibility(View.VISIBLE);
                mSpinner.setVisibility(View.GONE);

                mSwitch.setChecked((Boolean) event.value);

                mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> event.value = isChecked);
                break;
            case DPEvent.Type.PROP_VALUE:
            case DPEvent.Type.PROP_ENUM:
            case DPEvent.Type.PROP_BITMAP:
                mValue.setInputType(InputType.TYPE_CLASS_PHONE);
            case DPEvent.Type.PROP_STR:
            case DPEvent.Type.PROP_RAW:
                mValue.setVisibility(View.VISIBLE);
                mSwitch.setVisibility(View.GONE);
                mSpinner.setVisibility(View.GONE);

                if (event.type == DPEvent.Type.PROP_RAW) {
                    if (event.value == null) {
                        event.value = new byte[]{'0'};
                    }

                    mValue.setText(new String((byte[]) event.value));
                } else {
                    mValue.setText(event.value.toString());
                }

                mValue.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (event.type == DPEvent.Type.PROP_RAW) {
                            event.value = s.toString().getBytes(Charset.forName("UTF-8"));
                        } else if (event.type == DPEvent.Type.PROP_STR) {
                            event.value = s.toString();
                        } else {
                            try {
                                event.value = Integer.parseInt(s.toString());
                            } catch (NumberFormatException e) {
                                event.value = 0;
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                break;
        }
    }
}