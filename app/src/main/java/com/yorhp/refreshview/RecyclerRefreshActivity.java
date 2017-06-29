package com.yorhp.refreshview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import adapter.Adapter;
import myview.RefreshableViewRecycle;

public class RecyclerRefreshActivity extends AppCompatActivity {

    Adapter adapter;
    RefreshableViewRecycle refreshable_view;
    RecyclerView recycler_view;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refresh_recycler);
        adapter=new Adapter(this);
        refreshable_view= (RefreshableViewRecycle) findViewById(R.id.refreshable_view);
        recycler_view= (RecyclerView) findViewById(R.id.recycler_view);
        recycler_view.setAdapter(adapter);
        recycler_view.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        refreshable_view.setOnRefreshListener(new RefreshableViewRecycle.PullToRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                refreshable_view.finishRefreshing();
            }
        },1);

    }
}
