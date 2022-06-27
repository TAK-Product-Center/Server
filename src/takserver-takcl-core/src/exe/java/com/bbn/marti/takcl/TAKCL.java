package com.bbn.marti.takcl;

import com.bbn.marti.takcl.AppModules.*;
import com.bbn.marti.takcl.AppModules.generic.BaseAppModuleInterface;
import com.bbn.marti.takcl.cli.simple.SimpleMain;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Easy way to turn methods into CLI functionality with little work.
 * <p>
 * Created on 9/28/15.
 */
public class TAKCL {

	// Initialize a SimpleMain object
	private static final SimpleMain takcl = new SimpleMain();

	public static final boolean testMode;

	static {
		boolean tmpTestMode = false;
		try {
			// First get the information inserted into the manifest by the build script
			URL url = new URL(TAKCL.class.getResource(TAKCL.class.getSimpleName() + ".class").toString());
			URLConnection connection = url.openConnection();
			if (connection instanceof JarURLConnection) {
				JarURLConnection jarConnection = (JarURLConnection) connection;
				Manifest manifest = jarConnection.getManifest();
				Attributes mainAttributes = manifest.getMainAttributes();


				Set<Map.Entry<Object, Object>> attributeMap = mainAttributes.entrySet();

				// And if the "Developer-Mode" attribute is found and it is true, set it to test mode which includes
				// components that may be somewhat neglected at the moment.
				for (Map.Entry e : attributeMap) {
					if (e.getKey() instanceof Attributes.Name) {
						Attributes.Name aName = (Attributes.Name) e.getKey();
						if (aName.toString().equals("Developer-Mode")) {
							if (e.getValue() instanceof String) {
								String aValue = (String) e.getValue();
								if ("true".equals(aValue)) {
									tmpTestMode = true;
									break;
								}
							}

						}
					}
				}
				testMode = tmpTestMode;
			} else {
				// If it isn't a jar, why restrict?
				testMode = true;
			}

			// Add the module that is to be exposed in all modes (including the public release)
			takcl.addModule("tests", new TestRunnerModule());

			// And if it is in test mode, add the following modules
			if (testMode) {
				takcl.addModule("takclConfig", TAKCLConfigModule.getInstance());
				takcl.addModule("onlineInput", new OnlineInputModule());
				takcl.addModule("onlineFileAuth", new OnlineFileAuthModule());
				takcl.addModule("offlineConfig", new OfflineConfigModule());
				takcl.addModule("offlineFileAuth", new OfflineFileAuthModule());
				takcl.addModule("cotGenerator", new CotGeneratorModule());
				takcl.addModule("bashhelper", new BashHelperModule());
				takcl.addModule("client", new TAKClientModule());
				// Developer-only modules
				takcl.addModule("tests", new TestRunnerModule());
				takcl.addModule("testEnum", new TestEnumGeneratorModule());
				takcl.addModule("ehm", new EnvironmentHelperModule());
			}

			// Set the showDevCommands flag in the SimpleMain
			takcl.showDevCommands = testMode;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Map<String, BaseAppModuleInterface> moduleMap = takcl.moduleMap;

	public static void main(String[] args) {
		// Run the SimpleRunner instance with the arguments
		takcl.run(true, args);
	}
}
