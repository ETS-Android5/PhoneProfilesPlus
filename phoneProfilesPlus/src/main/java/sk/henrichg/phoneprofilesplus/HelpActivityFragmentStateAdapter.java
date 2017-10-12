package sk.henrichg.phoneprofilesplus;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

class HelpActivityFragmentStateAdapter extends FragmentStatePagerAdapter {

    private final int mNumOfTabs;

    HelpActivityFragmentStateAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return new ImportantInfoHelpFragment();
            case 1:
                return new QuickGuideHelpFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }

}
