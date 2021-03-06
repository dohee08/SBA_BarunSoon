package com.example.sba_project.GameRoomPkg;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.sba_project.Userdata.UserDataManager;
import com.example.sba_project.Util.UtilValues;

import java.util.ArrayList;

public class MyPagerAdapter extends FragmentPagerAdapter {
    private ArrayList<Fragment> mData;

    public MyPagerAdapter(FragmentManager fm) {
        super(fm);
        mData = new ArrayList<>();
        mData.add(new Game_Room_Frag());

        Game_Play_Frag tmpFrag = new Game_Play_Frag();
        UserDataManager.getInstance().setGamePlayFrag(tmpFrag);
        mData.add(tmpFrag);
    }

    @Override
    public Fragment getItem(int position) {
        return mData.get(position);
    }

    @Override
    public int getCount() {
        return mData.size();
    }
}
