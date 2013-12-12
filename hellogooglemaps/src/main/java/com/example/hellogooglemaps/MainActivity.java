package com.example.hellogooglemaps;

import com.actionbarsherlock.app.SherlockFragment;

public class MainActivity extends SingleFrameActivity {

    @Override
    protected SherlockFragment createFragment() {
        return  MainFragment.newInstance();
    }
}

