package org.minimallycorrect.typedconfig;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;

class ConfigFile {
	final List<Entry> orderedEntries = new ArrayList<>();
	final Map<String, String> entries = new HashMap<>();

	ConfigFile() {
	}

	@SneakyThrows
	ConfigFile(@NonNull Path p) {
		if (!Files.exists(p))
			return;

		for (String s : Files.readAllLines(p)) {
			s = s.trim();
			if (s.isEmpty() || s.indexOf('#') == 0)
				continue;
			val firstEquals = s.indexOf('=');
			if (firstEquals == -1)
				throw new ParseException("Couldn't find '=' in line " + s + " in " + p, 0);
			val name = s.substring(0, firstEquals).trim();
			val value = s.substring(firstEquals + 1).trim();
			entries.put(name, value);
		}
	}

	@SneakyThrows
	private static String serialize(Class<?> type, Object value) {
		try (val baos = new ByteArrayOutputStream()) {
			try (val oos = new ObjectOutputStream(baos)) {
				oos.writeObject(value);
				return Base64.getEncoder().encodeToString(baos.toByteArray());
			}
		}
	}

	private static String toString(Class<?> type, Object value) {
		if (value == null)
			return "";
		if (type == String.class)
			return (String) value;
		if (type.isPrimitive())
			return String.valueOf(value);
		if (Serializable.class.isAssignableFrom(type))
			return serialize(type, value);
		throw new UnsupportedOperationException("Unhandled type " + type.getCanonicalName());
	}

	@SneakyThrows
	void save(@NonNull Path p) {
		val temp = p.getParent().resolve(p.getFileName() + ".temp");
		try (val os = new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)), Charset.forName("UTF-8"))) {
			for (val entry : orderedEntries) {
				String value = entries.get(entry.name);
				if (value == null)
					value = "";
				os.write("#" + entry.description.replace("\n", "\n#") + "\n");
				os.write(entry.name + "=" + value + "\n");
			}
		}
		Files.move(temp, p, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	}

	Object getValue(@NonNull String name, @NonNull Class<?> type) {
		String value = entries.get(name);
		if (value == null || value.isEmpty())
			return null;
		if (type == String.class)
			return value;
		if (type == int.class)
			return Integer.parseInt(value);
		if (type == long.class)
			return Long.parseLong(value);
		if (type == float.class)
			return Float.parseFloat(value);
		if (type == double.class)
			return Double.parseDouble(value);
		if (type == byte.class)
			return Byte.parseByte(value);
		if (type == short.class)
			return Short.parseShort(value);
		if (type == boolean.class)
			return Boolean.parseBoolean(value);
		if (Serializable.class.isAssignableFrom(type))
			return deserialize(type, value);
		throw new UnsupportedOperationException("Unhandled type " + type.getCanonicalName());
	}

	@SneakyThrows
	private Object deserialize(Class<?> type, String value) {
		try (val ois = new SafeObjectInputStream(type.getName(), new ByteArrayInputStream(Base64.getDecoder().decode(value)))) {
			return ois.readObject();
		}
	}

	void setValue(@NonNull String name, @NonNull Class<?> type, @Nullable Object value) {
		entries.put(name, toString(type, value));
	}

	void addExpectedEntry(String name, String description) {
		orderedEntries.add(new Entry(name, description));
	}

	private static class SafeObjectInputStream extends ObjectInputStream {
		private final String allowedClass;

		SafeObjectInputStream(String allowedClass, InputStream inputStream) throws IOException {
			super(inputStream);
			this.allowedClass = allowedClass;
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException {
			if (!Objects.equals(osc.getName(), allowedClass))
				throw new IllegalAccessError("Only allowed to deserialize if class name is " + allowedClass);
			return super.resolveClass(osc);
		}
	}

	@AllArgsConstructor
	private static class Entry {
		final String name;
		final String description;
	}
}
