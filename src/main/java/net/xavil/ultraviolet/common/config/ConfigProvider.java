package net.xavil.ultraviolet.common.config;

public interface ConfigProvider {
	<T> T get(ConfigKey<T> key);
}
