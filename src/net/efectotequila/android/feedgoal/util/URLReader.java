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

import java.net.*;
import java.io.*;

public class URLReader {
    private URL _url;

    public URLReader(String url) throws MalformedURLException {
        this._url = new URL(url);
    }

    public StringBuilder read() throws IOException {
        StringBuilder sb = new StringBuilder(Math.round((float)(32334*1.2)));
        BufferedReader in = null;

        try {
            in = new BufferedReader(
                    new InputStreamReader(
                    _url.openStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                //System.out.println(inputLine);
                sb.append(inputLine).append(System.getProperty("line.separator"));
            }
        } finally {
            if(in != null) {
                in.close();
            }
        }

        return sb;
    }
}
