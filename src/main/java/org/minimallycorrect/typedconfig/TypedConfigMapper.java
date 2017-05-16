package org.minimallycorrect.typedconfig;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.lang.reflect.*;
import java.util.*;

class TypedConfigMapper<T> {
	final Class<T> clazz;
	final List<Entry> entries = new ArrayList<>();

	TypedConfigMapper(Class<T> clazz) {
		this.clazz = clazz;
		for (val field : clazz.getDeclaredFields()) {
			val annotations = field.getDeclaredAnnotations();
			val entryAnnotation = field.getDeclaredAnnotation(TypedConfig.Entry.class);
			if (entryAnnotation == null)
				continue;
			String name = entryAnnotation.name();
			if (name.isEmpty())
				name = field.getName();
			field.setAccessible(true);
			entries.add(new Entry(field, name, entryAnnotation.description()));
		}
	}

	@SneakyThrows
	T convert(ConfigFile configFile) {
		T t = clazz.newInstance();
		for (val entry : entries) {
			val field = entry.field;
			val value = configFile.getValue(entry.name, field.getType());
			if (value != null)
				field.set(t, value);
		}
		return t;
	}

	@SneakyThrows
	ConfigFile convert(T config) {
		ConfigFile configFile = new ConfigFile();
		T defaultConfig = clazz.newInstance();
		for (val entry : entries) {
			configFile.addExpectedEntry(entry.name, entry.description);
			val field = entry.field;
			val original = field.get(defaultConfig);
			val current = field.get(config);
			if (!Objects.equals(original, current))
				configFile.setValue(entry.name, field.getType(), current);
		}
		return configFile;
	}

	@AllArgsConstructor
	private static class Entry {
		final Field field;
		final String name;
		final String description;
	}
}
