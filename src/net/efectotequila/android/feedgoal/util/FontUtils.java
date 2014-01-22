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

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FontUtils {

	private static Typeface robotoTypeFace;

	public static void setRobotoFont(Context context, View view) {
		if (robotoTypeFace == null) {
			robotoTypeFace = Typeface.createFromAsset(context.getAssets(),
					"fonts/Roboto-Regular.ttf");
		}
		setFont(view, robotoTypeFace);
	}

	private static void setFont(View view, Typeface robotoTypeFace) {
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				setFont(((ViewGroup) view).getChildAt(i), robotoTypeFace);
			}
		} else if (view instanceof TextView) {
			((TextView) view).setTypeface(robotoTypeFace);
		}
	}
}
