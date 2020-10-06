package com.farmerbb.taskbar.shadow;

import android.os.Build;
import android.service.quicksettings.TileService;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowTileService;

@Implements(value = TileService.class, minSdk = Build.VERSION_CODES.N)
public class TaskbarShadowTileService extends ShadowTileService {
    @Implementation
    protected final void unlockAndRun(Runnable runnable) {
        setLocked(false);
        if (runnable != null) {
            runnable.run();
        }
    }
}
