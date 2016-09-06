package com.example.administrator.debug;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ViewPager viewPager;
    List<Fragment> mList;
    MyAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager= (ViewPager) findViewById(R.id.pager);
        mList=new ArrayList<Fragment>();
        for (int i=1;i<4;i++){
            Bundle bundle=new Bundle();
            bundle.putString("text","第"+i+"页");
            MyFragment myFragment=new MyFragment();
            myFragment.setArguments(bundle);
            mList.add(myFragment);
        }
        adapter=new MyAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

    }

    public class MyAdapter extends FragmentPagerAdapter{

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mList.get(position);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        // start
        // 可以删除这段代码看看，数据源更新而viewpager不更新的情况
        private int mChildCount = 0;

        @Override
        public void notifyDataSetChanged() {
            // 重写这个方法，取到子Fragment的数量，用于下面的判断，以执行多少次刷新
            mChildCount = getCount();
            super.notifyDataSetChanged();
        }
        @Override
        public int getItemPosition(Object object) {
            if ( mChildCount > 0) {
                // 这里利用判断执行若干次不缓存，刷新
                mChildCount --;
                // 返回这个是强制ViewPager不缓存，每次滑动都刷新视图
                return POSITION_NONE;
            }
            // 这个则是缓存不刷新视图
            return super.getItemPosition(object);
        }
        // end
    }

    public void btn_click(View v){
        update();
    }

    public void update(){

        // start
        // 可以删除这段代码看看，数据源更新而viewpager不更新的情况
        // 在数据源更新前增加的代码，将上一次数据源的fragment对象从FragmentManager中删除
        if (viewPager.getAdapter() != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            List<Fragment> fragments = fm.getFragments();
            if(fragments != null && fragments.size() >0){
                for (int i = 0; i < fragments.size(); i++) {
                    ft.remove(fragments.get(i));
                }
            }
            ft.commit();
        }
        // End

        mList.clear();
        for (int i=4;i<7;i++){
            Bundle bundle=new Bundle();
            bundle.putString("text","第"+i+"页");
            MyFragment myFragment=new MyFragment();
            myFragment.setArguments(bundle);
            mList.add(myFragment);
        }
        // 重写adapter的notifyDataChanged方法
        adapter.notifyDataSetChanged();
    }
}
