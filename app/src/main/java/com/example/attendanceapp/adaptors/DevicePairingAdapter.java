package com.example.attendanceapp.adaptors;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.attendanceapp.R;
import com.example.attendanceapp.models.WhitelistedDevice;
import java.util.List;

public class DevicePairingAdapter extends RecyclerView.Adapter<DevicePairingAdapter.DeviceViewHolder> {

    private List<WhitelistedDevice> devices;
    private OnDeviceStatusChangeListener statusChangeListener;

    public interface OnDeviceStatusChangeListener {
        void onDeviceStatusChanged(WhitelistedDevice device, boolean enabled);
    }

    public DevicePairingAdapter(List<WhitelistedDevice> devices, OnDeviceStatusChangeListener listener) {
        this.devices = devices;
        this.statusChangeListener = listener;
    }

    public void updateDevices(List<WhitelistedDevice> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_pairing, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        WhitelistedDevice device = devices.get(position);
        holder.bind(device, statusChangeListener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDeviceName;
        private TextView tvMacAddress;
        private TextView tvStudentInfo;
        private ImageView ivDeviceIcon;
        private Switch switchEnabled;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvMacAddress = itemView.findViewById(R.id.tvMacAddress);
            tvStudentInfo = itemView.findViewById(R.id.tvStudentInfo);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
            switchEnabled = itemView.findViewById(R.id.switchEnabled);
        }

        public void bind(WhitelistedDevice device, OnDeviceStatusChangeListener listener) {
            tvDeviceName.setText(device.getDeviceName());
            tvMacAddress.setText(device.getMacAddress());
            tvStudentInfo.setText(device.getStudentName() + " (" + device.getStudentId() + ")");

            // Set device icon based on type
            ivDeviceIcon.setImageResource(
                    device.getDeviceType().equals("phone") ?
                            R.drawable.ic_phone : R.drawable.ic_tablet
            );

            switchEnabled.setChecked(device.isEnabled());
            switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onDeviceStatusChanged(device, isChecked);
                }
            });
        }
    }
}