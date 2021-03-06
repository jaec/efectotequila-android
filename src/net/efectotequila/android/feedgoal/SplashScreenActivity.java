/*
 * efectotequila is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * efectotequila is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with efectotequila.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.efectotequila.android.feedgoal;

import net.efectotequila.android.feedgoal.storage.DbFeedAdapter;
import net.efectotequila.android.feedgoal.storage.SharedPreferencesHelper;
import net.efectotequila.android.feedgoal.util.DrawableUtils;
import net.efectotequila.android.feedgoal.util.FontUtils;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class SplashScreenActivity extends Activity {
	
	private static final String LOG_TAG = "SplashScreenActivity";
	private final Handler mHandler = new Handler();
 
    private final Runnable mPendingLauncherRunnable = new Runnable() {
        public void run() {
        	DbFeedAdapter mDbFeedAdapter = new DbFeedAdapter(SplashScreenActivity.this);
            mDbFeedAdapter.open();
            
            Intent intent = new Intent(SplashScreenActivity.this, FeedTabActivity.class);
            startActivity(intent);
            
            mDbFeedAdapter.close();
            finish();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        
        // Track installations of the app (not the device!)
        if (SharedPreferencesHelper.getUniqueId(this) == null)
        	SharedPreferencesHelper.setUniqueId(this);
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferencesHelper.setPrefTabFeedId(this, SharedPreferencesHelper.getPrefStartChannel(this));
        
        // Prepare show Startup Dialog on update
        if (SharedPreferencesHelper.showPrefStartupDialogOnUpdate(this) && SharedPreferencesHelper.isNewUpdate(this)) {
        	SharedPreferencesHelper.setPrefStartupDialogOnUpdate(this, true);
        	// Fix bug inherited from version 1.6 (version code 17) and affecting new versions.
        	// Bug: dialog on install was displayed after each refresh.
        	SharedPreferencesHelper.setPrefStartupDialogOnInstall(this, false);
        } else
        	SharedPreferencesHelper.setPrefStartupDialogOnUpdate(this, false);
        
        setContentView(R.layout.splash_screen);
        
        Drawable backgroundDrawable = getResources().getDrawable(R.drawable.splash_background);
        backgroundDrawable.setDither(true);
        findViewById(android.R.id.content).setBackgroundDrawable(backgroundDrawable);
        mHandler.postDelayed(mPendingLauncherRunnable, SharedPreferencesHelper.getSplashDuration(this));
        
        ViewGroup godfatherView = (ViewGroup) this.getWindow().getDecorView();
		FontUtils.setRobotoFont(this, godfatherView);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mPendingLauncherRunnable);
    }
    
    @Override
	protected void onDestroy() {
		View view = findViewById(R.id.content);
		
		if (view != null) {
			DrawableUtils.unbindDrawables(view);
		}
		super.onDestroy();
	}
}
