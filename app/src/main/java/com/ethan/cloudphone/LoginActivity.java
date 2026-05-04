package com.ethan.cloudphone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ethan.cloudphone.api.ApiClient;

public class LoginActivity extends Activity {
    private static final String TAG = "LoginActivity";
    
    private EditText etServerUrl;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnConnect;
    private Button btnLogin;
    private View serverUrlLayout;
    private View loginLayout;
    private CheckBox cbSaveUrl;
    private ApiClient apiClient;
    private boolean serverUrlSaved = false;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        apiClient = ApiClient.getInstance(this);
        
        // 检查是否已登录，已登录直接跳 MainActivity
        if (apiClient.hasServerUrl() && apiClient.isLoggedIn()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_login);
        
        initViews();
        
        // 检查是否已保存服务器地址
        if (apiClient.hasServerUrl()) {
            etServerUrl.setText(apiClient.getServerUrl());
            showLoginLayout();
        } else {
            showServerUrlLayout();
        }
    }
    
    private void initViews() {
        etServerUrl = findViewById(R.id.et_server_url);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnConnect = findViewById(R.id.btn_connect);
        btnLogin = findViewById(R.id.btn_login);
        serverUrlLayout = findViewById(R.id.layout_server_url);
        loginLayout = findViewById(R.id.layout_login);
        cbSaveUrl = findViewById(R.id.cb_save_url);
        
        btnConnect.setOnClickListener(v -> connectToServer());
        btnLogin.setOnClickListener(v -> doLogin());
    }
    
    private void showServerUrlLayout() {
        serverUrlLayout.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.GONE);
    }
    
    private void showLoginLayout() {
        serverUrlLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.VISIBLE);
    }
    
    private void connectToServer() {
        String serverUrl = etServerUrl.getText().toString().trim();
        
        if (TextUtils.isEmpty(serverUrl)) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 清理 URL
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        
        // 验证 URL 格式
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "http://" + serverUrl;
        }
        
        btnConnect.setEnabled(false);
        btnConnect.setText("连接中...");
        
        apiClient.setServerUrl(serverUrl);
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean connected = apiClient.testConnection();
            btnConnect.setEnabled(true);
            btnConnect.setText("连接");
            
            if (connected) {
                // 保存服务器地址
                if (cbSaveUrl.isChecked()) {
                    serverUrlSaved = true;
                }
                showLoginLayout();
                Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无法连接到服务器，请检查地址", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }
    
    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");
        
        new Thread(() -> {
            try {
                ApiClient.LoginResult result = apiClient.login(username, password);
                
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("登录");
                    
                    if (result.success) {
                        // 登录成功，跳转到设备列表
                        Intent intent = new Intent(LoginActivity.this, DeviceListActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Login failed", e);
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("登录");
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}