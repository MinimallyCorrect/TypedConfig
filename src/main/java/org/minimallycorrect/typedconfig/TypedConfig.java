package org.minimallycorrect.typedconfig;

import lombok.NonNull;
import lombok.val;

import javax.annotation.Nullable;
import java.lang.annotation.*;
import java.nio.file.*;
import java.util.*;

public class TypedConfig<T> {
	private final TypedConfigMapper<T> mapper;
	private T lastConfig;
	private Path lastPath;

	private TypedConfig(@NonNull TypedConfigMapper<T> mapper, @Nullable Path p) {
		this.mapper = mapper;
		this.lastPath = p;
	}

	public static <C> TypedConfig<C> of(@NonNull Class<C> configClass, Path p) {
		return new TypedConfig<>(new TypedConfigMapper<>(configClass), p);
	}

	public static <C> TypedConfig<C> of(@NonNull Class<C> configClass) {
		return new TypedConfig<>(new TypedConfigMapper<>(configClass), null);
	}

	public @NonNull
	T load() {
		return load(lastPath);
	}

	public @NonNull
	T load(@NonNull Path p) {
		val configFile = new ConfigFile(p);
		val config = mapper.convert(configFile);
		lastConfig = mapper.convert(configFile);
		return config;
	}

	public boolean save(T config) {
		return save(config, lastPath);
	}

	public boolean save(T config, @NonNull Path p) {
		if (Objects.equals(p, lastPath) && Objects.equals(lastConfig, config))
			return false;

		lastPath = p;
		val file = mapper.convert(config);
		file.save(p);
		return true;
	}

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Entry {
		String description();

		String name() default "";
	}
}
