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

package net.efectotequila.android.feedgoal.util;


import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

public class DrawableUtils {
	public static final String DEFAULT_DEBUG_TAG = "DrawableUtils";

	public static void unbindDrawables(View view) {
		if (view != null) {
			if (view.getBackground() != null) {
				view.getBackground().setCallback(null);

				if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
					for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
						unbindDrawables(((ViewGroup) view).getChildAt(i));
					}
					((ViewGroup) view).removeAllViews();
				}
			}
		} else {
			Log.w(DEFAULT_DEBUG_TAG, "unbindDrawables view is null");
		}
	}
	
}
