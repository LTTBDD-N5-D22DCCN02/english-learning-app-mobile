//package com.estudy.app.controller;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
//import androidx.appcompat.app.AppCompatActivity;
//import com.estudy.app.R;
//import com.estudy.app.utils.TokenManager;
//
//public class SplashActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_splash);
//
//        // Xóa token cũ để bắt buộc đăng nhập lại mỗi lần mở app
//        new TokenManager(this).clearToken();
//
//        new Handler().postDelayed(() -> {
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//        }, 2000);
//    }
//}

package com.estudy.app.controller;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.estudy.app.R;
import com.estudy.app.utils.TokenManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TokenManager tokenManager = new TokenManager(this);

        new Handler().postDelayed(() -> {
            // Nếu đã có token → vào thẳng Home, không cần login lại
            if (tokenManager.hasToken()) {
                startActivity(new Intent(this, HomeActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, 1500);
    }
}