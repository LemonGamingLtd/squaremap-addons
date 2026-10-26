package xyz.jpenilla.squaremap.addon.factionsuuid.web;

import com.google.gson.Gson;
import org.bukkit.plugin.Plugin;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds a "find faction" box to the squaremap web page. Each world gets a tiles/{world}/faction-claims.json
 * listing every faction's claims, and lg-faction-search.js reads it to highlight one faction's claims.
 */
public final class FactionSearch {
    private static final String SCRIPT = "lg-faction-search.js";
    private static final Gson GSON = new Gson();

    private final Plugin plugin;

    public FactionSearch(Plugin plugin) {
        this.plugin = plugin;
    }

    public record FactionClaims(String id, String tag, String color, List<int[]> claims, List<long[]> raid) {
    }

    // squaremap re-extracts its web files (index.html included) on every start and /squaremap reload, so this runs on each update
    public synchronized void install() {
        final Path webDir = SquaremapProvider.get().webDir();
        try (final InputStream in = this.plugin.getResource("web/" + SCRIPT)) {
            if (in == null) {
                throw new IOException("web/" + SCRIPT + " missing from the jar");
            }
            final String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            final Path scriptFile = webDir.resolve(SCRIPT);
            if (!Files.exists(scriptFile) || !Files.readString(scriptFile).equals(script)) {
                write(scriptFile, script);
            }

            // versioned by content so browsers don't keep a cached copy of an older script
            final String tag = "<script src=\"./" + SCRIPT + "?v=" + Integer.toHexString(script.hashCode()) + "\"></script>";
            final Path index = webDir.resolve("index.html");
            final String html = Files.readString(index);
            if (!html.contains(tag)) {
                // it has to run before squaremap's own module script so it can catch the Leaflet map being created
                final String withoutOld = html.replaceAll("\\n[ \\t]*<script src=\"\\./" + Pattern.quote(SCRIPT) + "[^\\n]*", "");
                write(index, withoutOld.replaceFirst("<head>", "<head>\n        " + Matcher.quoteReplacement(tag)));
            }
        } catch (final IOException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to install the faction search script", e);
        }
    }

    public void writeClaims(MapWorld world, List<FactionClaims> factions) {
        final Path file = SquaremapProvider.get().webDir()
            .resolve("tiles")
            .resolve(world.identifier().asString().replace(':', '_'))
            .resolve("faction-claims.json");
        try {
            write(file, GSON.toJson(Map.of("updated", System.currentTimeMillis(), "factions", factions)));
        } catch (final IOException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to write " + file, e);
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        final Path tmp = file.resolveSibling("." + file.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
