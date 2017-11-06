package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

/**
 * Created by lawren on 02/11/17.
 */

public class PhotoPageActivity extends SingleFragmentActivity {
   private PhotoPageFragment mPhotoPageFragment;

    public static Intent newIntent(Context context, Uri photoPageUri){
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(photoPageUri);
        return intent;
    }

    @Override
    protected Fragment createFragment(){
      mPhotoPageFragment = PhotoPageFragment.newInstance(getIntent().getData());
      return mPhotoPageFragment;
    }

    @Override
    public void onBackPressed() {
        if (mPhotoPageFragment.onBackPressed()) {
            return;
        } else {
            super.onBackPressed();
        }
    }
}
