package dev.revivalo.playerwarps.configuration;

import com.tchristofferson.configupdater.ConfigUpdater;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

public class YamlFile {

	private final File file;
	private final UpdateMethod updateMethod;
	private final String filePath;
	private final YamlConfiguration configuration;

	/**
	 * False when the file exists but could not be read. Its content is unknown then, so it must
	 * never be overwritten - saving the empty configuration would wipe it.
	 */
	private boolean loaded = true;

	public YamlFile(final String filePath, final File folder, final UpdateMethod updateMethod) {
		file = new File(folder, filePath);
		this.updateMethod = updateMethod;
		this.filePath = filePath;

		boolean update = true;
		switch (this.updateMethod){
			case NEVER:
				update = false;
				break;
			case ON_LOAD:
				update = !file.exists();
				break;
		}

		Validate.notNull(file, "File cannot be null");

		configuration = new YamlConfiguration();

		try {
			configuration.load(file);
		} catch (FileNotFoundException ex) {
		} catch (IOException | InvalidConfigurationException ex) {
			loaded = false;
			PlayerWarpsPlugin.get().getLogger().log(Level.SEVERE, "Cannot load " + file
					+ ". The file will not be written to until it is fixed and the plugin is restarted.", ex);
			backUpUnreadableFile();
			return;
		}

		// Only creates a missing file. An existing one must not be written back right after
		// loading: Bukkit drops whatever it could not deserialize (a warp, or a location in a
		// world that is not loaded yet), so the rewrite would delete it from the file.
		if (!file.exists()) {
			save();
		}

		configuration.options().copyDefaults(true);

		if (update) {
			try {
				if (PlayerWarpsPlugin.get().getResource(filePath) != null) {
					ConfigUpdater.update(PlayerWarpsPlugin.get(), filePath, file, Collections.emptyList());
				}
				reload();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public YamlConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Reads the file as plain maps and lists, without turning anything into Bukkit objects.
	 * Serialized objects stay maps with their {@code ==} type key, so values Bukkit cannot
	 * deserialize at the moment (a location in a world that is not loaded) are not lost.
	 *
	 * @return the top level mapping, or an empty map if the file is missing or unreadable
	 */
	public Map<String, Object> readRaw() {
		if (!file.exists()) {
			return Collections.emptyMap();
		}

		final LoaderOptions options = new LoaderOptions();
		// The same limit Bukkit uses - the default would refuse large data files.
		options.setCodePointLimit(Integer.MAX_VALUE);

		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			final Object root = new Yaml(new SafeConstructor(options)).load(reader);
			if (!(root instanceof Map<?, ?> map)) {
				return Collections.emptyMap();
			}

			final Map<String, Object> result = new LinkedHashMap<>();
			map.forEach((key, value) -> result.put(String.valueOf(key), value));
			return result;
		} catch (IOException | RuntimeException ex) {
			PlayerWarpsPlugin.get().getLogger().log(Level.WARNING, "Cannot read " + file, ex);
			return Collections.emptyMap();
		}
	}

	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * Keeps a copy of a file that could not be read, so the original content survives even if
	 * someone deletes the file to get the plugin running again.
	 */
	private void backUpUnreadableFile() {
		final File backup = new File(file.getParentFile(), file.getName() + ".broken-" + System.currentTimeMillis());
		try {
			Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
			PlayerWarpsPlugin.get().getLogger().severe("A copy of the unreadable file was saved as " + backup.getName());
		} catch (IOException e) {
			PlayerWarpsPlugin.get().getLogger().log(Level.SEVERE, "Cannot back up " + file, e);
		}
	}

	public void save() {
		if (!loaded) {
			PlayerWarpsPlugin.get().getLogger().warning("Not saving " + file + ", it could not be loaded.");
			return;
		}

		try {
			this.configuration.save(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes already serialized content to the file. Kept separate from {@link #save()} so the
	 * write can be performed off the main thread without serializing there too. Synchronized to
	 * keep two overlapping writes from interleaving.
	 * <p>
	 * The content goes to a temporary file first, which then replaces the original in one step.
	 * A crash in the middle of the write leaves the previous file intact instead of a cut off
	 * one. The previous version is kept as a {@code .bak} file.
	 */
	public synchronized void write(String contents) {
		if (!loaded) {
			PlayerWarpsPlugin.get().getLogger().warning("Not saving " + file + ", it could not be loaded.");
			return;
		}

		try {
			final File parent = file.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}

			final Path target = file.toPath();
			final Path temporary = target.resolveSibling(file.getName() + ".tmp");
			Files.writeString(temporary, contents, StandardCharsets.UTF_8);

			if (Files.exists(target)) {
				Files.copy(target, target.resolveSibling(file.getName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
			}

			try {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException ex) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			PlayerWarpsPlugin.get().getLogger().log(Level.SEVERE, "Cannot save " + file, e);
		}
	}

	public void reload() {
		try {
			configuration.load(file);
			loaded = true;
		} catch (InvalidConfigurationException | IOException ex) {
			ex.printStackTrace();
		}
	}

	public File getFile() {
		return this.file;
	}

	public UpdateMethod getUpdateMethod() {
		return this.updateMethod;
	}

	public String getFilePath() {
		return this.filePath;
	}

	public enum UpdateMethod {
		EVERYTIME, ON_LOAD, NEVER
	}
}
