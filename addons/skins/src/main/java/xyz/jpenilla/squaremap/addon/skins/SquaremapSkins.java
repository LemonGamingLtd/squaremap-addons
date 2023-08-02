package xyz.jpenilla.squaremap.addon.skins;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import xyz.jpenilla.squaremap.addon.common.scheduler.WrappedRunnable;
import xyz.jpenilla.squaremap.addon.common.AddonJavaPlugin;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

public final class SquaremapSkins extends AddonJavaPlugin {
    private static SquaremapSkins instance;
    private static File skinsDir;

    public SquaremapSkins() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        skinsDir = new File(SquaremapProvider.get().webDir().toFile(), "skins");
        if (!skinsDir.exists() && !skinsDir.mkdirs()) {
            getLogger().severe("Could not create skins directory!");
            getLogger().severe("Check your file permissions and try again");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerJoin(PlayerJoinEvent event) {
                new FetchSkinURL(event.getPlayer()).runTaskLater(instance.scheduler(), 250L, TimeUnit.MILLISECONDS);
            }
        }, this);

        int interval = getConfig().getInt("update-interval", 60);
        new UpdateTask().runTaskTimer(instance.scheduler(), interval / 20L, interval / 20L, TimeUnit.SECONDS);
    }

    private static class UpdateTask extends WrappedRunnable {
        @Override
        public void run() {
            Bukkit.getOnlinePlayers().forEach(player ->
                new FetchSkinURL(player).runTask(instance.scheduler()));
        }
    }

    private static final class FetchSkinURL extends WrappedRunnable {
        private final Player player;

        private FetchSkinURL(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                return;
            }
            String url = getTexture(player);
            if (url == null || url.isEmpty()) {
                return;
            }
            String name = player.getName();
            new SaveSkin(name, url).runTaskAsynchronously(instance.scheduler());
        }
    }

    private static final class SaveSkin extends WrappedRunnable {
        private final String name;
        private final String url;

        private SaveSkin(String name, String url) {
            this.name = name;
            this.url = url;
        }

        @Override
        public void run() {
            instance.saveTexture(name, url);
        }
    }

    private static String getTexture(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        for (ProfileProperty property : profile.getProperties()) {
            if (property.getName().equals("textures")) {
                try {
                    String data = property.getValue();
                    byte[] base64 = Base64.getDecoder().decode(data);
                    String json = new String(base64);
                    JSONObject obj = (JSONObject) new JSONParser().parse(json);
                    JSONObject obj2 = (JSONObject) obj.get("textures");
                    JSONObject obj3 = (JSONObject) obj2.get("SKIN");
                    return (String) obj3.get("url");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void saveTexture(String name, String url) {
        try {
            BufferedImage img = ImageIO.read(new URL(url)).getSubimage(8, 8, 8, 8);
            File file = new File(skinsDir, name + ".png");
            ImageIO.write(img, "png", file);
        } catch (Exception e) {
            this.getSLF4JLogger().info("Could not save texture {} to {}", url, new File(skinsDir, name + ".png"), e);
        }
    }
}
