package com.ethan.cloudphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ethan.cloudphone.api.ApiClient;

import org.json.JSONObject;

import java.util.List;

public class DeviceListActivity extends Activity {
    private static final String TAG = "DeviceListActivity";
    
    private GridLayout gridLayout;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ImageButton btnAdd;
    private Button btnLogout;
    private ApiClient apiClient;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        
        apiClient = ApiClient.getInstance(this);
        
        // 检查登录状态
        if (!apiClient.isLoggedIn()) {
            goToLogin();
            return;
        }
        
        initViews();
        loadDevices();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回时刷新设备列表
        if (apiClient.isLoggedIn()) {
            loadDevices();
        }
    }
    
    private void initViews() {
        gridLayout = findViewById(R.id.grid_devices);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        btnAdd = findViewById(R.id.btn_add);
        btnLogout = findViewById(R.id.btn_logout);
        
        btnAdd.setOnClickListener(v -> showRedeemDialog());
        btnLogout.setOnClickListener(v -> logout());
    }
    
    private void loadDevices() {
        showLoading(true);
        
        new Thread(() -> {
            try {
                List<ApiClient.Device> devices = apiClient.getDevices();
                
                runOnUiThread(() -> {
                    showLoading(false);
                    displayDevices(devices);
                });
            } catch (Exception e) {
                Log.e(TAG, "Load devices failed", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(DeviceListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    if (e.getMessage().contains("登录已过期")) {
                        goToLogin();
                    }
                });
            }
        }).start();
    }
    
    private void displayDevices(List<ApiClient.Device> devices) {
        gridLayout.removeAllViews();
        
        if (devices == null || devices.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            gridLayout.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            gridLayout.setVisibility(View.VISIBLE);
            
            // 每行3个设备
            int columnCount = 3;
            gridLayout.setColumnCount(columnCount);
            
            for (ApiClient.Device device : devices) {
                View itemView = createDeviceItem(device);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.setMargins(8, 8, 8, 8);
                itemView.setLayoutParams(params);
                gridLayout.addView(itemView);
            }
        }
    }
    
    private View createDeviceItem(ApiClient.Device device) {
        View view = getLayoutInflater().inflate(R.layout.item_device, gridLayout, false);
        
        TextView tvName = view.findViewById(R.id.tv_device_name);
        ImageView ivStatus = view.findViewById(R.id.iv_status);
        Button btnConnect = view.findViewById(R.id.btn_connect);
        
        tvName.setText(device.deviceName);
        
        // 设置状态圆点颜色
        if (device.allocated) {
            ivStatus.setImageResource(R.drawable.status_online);
        } else {
            ivStatus.setImageResource(R.drawable.status_offline);
        }
        
        btnConnect.setOnClickListener(v -> {
            // 跳转到主页面投屏
            Intent intent = new Intent(DeviceListActivity.this, MainActivity.class);
            intent.putExtra("device_ip", device.ip);
            intent.putExtra("device_port", device.port);
            intent.putExtra("device_name", device.deviceName);
            startActivity(intent);
        });
        
        return view;
    }
    
    private void showRedeemDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_redeem, null);
        EditText etCode = dialogView.findViewById(R.id.et_redeem_code);
        
        new AlertDialog.Builder(this)
            .setTitle("兑换设备")
            .setView(dialogView)
            .setPositiveButton("兑换", (dialog, which) -> {
                String code = etCode.getText().toString().trim();
                if (code.isEmpty()) {
                    Toast.makeText(this, "请输入兑换码", Toast.LENGTH_SHORT).show();
                    return;
                }
                redeemDevice(code);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void redeemDevice(String code) {
        progressBar.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            try {
                ApiClient.Device device = apiClient.activateDevice(code);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(DeviceListActivity.this, "兑换成功！", Toast.LENGTH_SHORT).show();
                    // 刷新列表
                    loadDevices();
                });
            } catch (Exception e) {
                Log.e(TAG, "Redeem failed", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(DeviceListActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void logout() {
        new AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                apiClient.logout();
                goToLogin();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            tvEmpty.setVisibility(View.GONE);
        }
    }
}