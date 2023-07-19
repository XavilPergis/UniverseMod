package net.xavil.ultraviolet.debug;

public interface ConfigProvider {
	<T> T get(ConfigKey<T> key);
}
