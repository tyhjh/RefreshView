package com.yorhp.refreshview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button btn_reycler,btn_list,btn_NestedScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_reycler= (Button) findViewById(R.id.btn_reycler);
        btn_list= (Button) findViewById(R.id.btn_list);
        btn_NestedScrollView= (Button) findViewById(R.id.btn_NestedScrollView);
        btn_reycler.setOnClickListener(this);
        btn_list.setOnClickListener(this);
        btn_NestedScrollView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_reycler:
                startActivity(new Intent(MainActivity.this,RecyclerRefreshActivity.class));
                break;
            case R.id.btn_list:
                startActivity(new Intent(MainActivity.this,ListRefreshActivity.class));
                break;
            case R.id.btn_NestedScrollView:
                startActivity(new Intent(MainActivity.this,NestedRefreshActivity.class));
                break;
        }
    }
}
