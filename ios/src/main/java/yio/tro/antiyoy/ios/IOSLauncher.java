package yio.tro.antiyoy.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;
import yio.tro.antiyoy.PlatformType;
import yio.tro.antiyoy.YioGdxGame;

/**
 * Точка входа под iOS (MobiVM). Собирается только на macOS-раннере.
 */
public class IOSLauncher extends IOSApplication.Delegate {

    @Override
    protected IOSApplication createApplication() {
        YioGdxGame.platformType = PlatformType.ios;

        IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();

        return new IOSApplication(new YioGdxGame(), configuration);
    }


    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}
