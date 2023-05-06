package net.xavil.universal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class PreLaunchMod implements PreLaunchEntrypoint {

	public static final Logger LOGGER = LoggerFactory.getLogger("universal-prelaunch");

	@Override
	public void onPreLaunch() {
//		var javaLibPath = System.getProperty("java.library.path");
//		LOGGER.info("Attempting to load RenderDoc from " + javaLibPath);
//
//		boolean success = false;
//		success |= tryLoadLibary("renderdoc");
//		success |= tryLoadLibary("renderdoc.so");
//		success |= tryLoadLibary("renderdoc.dll");
//		success |= tryLoadLibary("/usr/lib/librenderdoc.so");
//
//		if (success) {
//			LOGGER.info("Loaded RenderDoc!");
//		} else {
//			LOGGER.info("Failed to load RenderDoc");
//		}
	}

	private static boolean tryLoadLibary(String lib) {
//		try {
//			System.loadLibrary(lib);
//			return true;
//		} catch (Throwable t) {
//		}
		return false;
	} 

}
