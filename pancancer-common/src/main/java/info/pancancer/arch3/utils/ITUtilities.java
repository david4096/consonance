package info.pancancer.arch3.utils;

import java.io.File;
import java.io.IOException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;

/**
 * Utility methods for testing.
 *
 * @author dyuen
 */
public class ITUtilities {

    /**
     * Clears database state and known queues for testing.
     *
     * @throws IOException
     */
    public static void clearState() throws IOException {
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = Utilities.parseConfig(configFile.getAbsolutePath());
        Utilities.clearState(parseConfig);
    }

}
