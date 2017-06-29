package com.yorhp.refreshview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import myview.RefreshableViewNested;

public class NestedRefreshActivity extends AppCompatActivity {

    RefreshableViewNested refreshable_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested_refresh);
        refreshable_view= (RefreshableViewNested) findViewById(R.id.refreshable_view);
        refreshable_view.setOnRefreshListener(new RefreshableViewNested.PullToRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                refreshable_view.finishRefreshing();
            }
        },2);
    }
}
