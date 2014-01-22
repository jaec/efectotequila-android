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

package net.efectotequila.android.feedgoal.storage;

import android.app.backup.BackupManager;
import android.content.Context;

public class BackupManagerCompatWrapper {
    private BackupManager wrappedBackupManagerInstance;

    /* class initialization fails when this throws an exception */
    static {
        try {
            Class.forName("android.app.backup.BackupManager");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /* calling here forces class initialization */
    public static void checkAvailable() {}

    public void dataChanged() {

    	wrappedBackupManagerInstance.dataChanged();
    }

    public BackupManagerCompatWrapper(Context ctx) {
    	wrappedBackupManagerInstance = new BackupManager(ctx);
    }
}
