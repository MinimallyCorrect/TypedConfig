package org.minimallyorrect.typedconfig.test;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import org.minimallycorrect.typedconfig.TypedConfig;
import org.minimallycorrect.typedconfig.TypedConfig.Entry;

import java.nio.file.*;

public class ConfigTest {
	@SneakyThrows
	@Test
	public void loadConfig() {
		val target = Paths.get("./src/test/resources/example.cfg");
		Files.createDirectories(target.getParent());
		val typedConfig = TypedConfig.of(Config.class, target);
		val config = new Config();
		config.pineappleCount = 5;
		typedConfig.save(config);
		Assert.assertEquals(config, typedConfig.load());
	}

	@EqualsAndHashCode
	@ToString
	public static class Config implements Cloneable {
		@Entry(description = "Number of pineapples to hold")
		int pineappleCount = 2;
	}
}
