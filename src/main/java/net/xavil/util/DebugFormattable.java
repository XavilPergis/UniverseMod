package net.xavil.util;

public interface DebugFormattable {

	interface DebugConsumer {
		
	}

	void writeDebugInfo(DebugConsumer output);

}
