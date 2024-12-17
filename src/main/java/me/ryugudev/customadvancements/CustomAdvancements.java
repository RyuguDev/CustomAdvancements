package me.ryugudev.customadvancements;

import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CustomAdvancements extends JavaPlugin implements Listener {

    private static final String ADVANCEMENT_FILE = "advancements.json";
    private Map<String, String> advancements = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        loadAdvancements();
    }

    @Override
    public void onDisable() {
        saveAdvancements();
    }

    @EventHandler
    public void onLoad(ServerLoadEvent event) {
        disableVanillaAdvancements();
    }

    @EventHandler
    public void onPlayerAdvance(PlayerAdvancementDoneEvent event) {
        String key = event.getAdvancement().getKey().getKey();
        if (key.contains("root") || key.contains("recipes")) return;

        Player player = event.getPlayer();
        String title = advancements.getOrDefault(key, key);

        String message = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("message", "{NAME} hat {ADV} erreicht!")
                        .replace("{NAME}", player.getDisplayName())
                        .replace("{USERNAME}", player.getName())
                        .replace("{ADV}", title));

        Bukkit.broadcastMessage(message);
    }

    private void disableVanillaAdvancements() {
        Bukkit.getLogger().info("Disabling vanilla advancement messages...");
        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        }
    }

    private void loadAdvancements() {
        advancements.clear();

        // Lade Fortschritte aus Minecraft
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement adv = iterator.next();
            String key = adv.getKey().getKey();
            String name = getAdvancementDisplayName(adv);
            advancements.put(key, name);
        }

        // Speichere Fortschritte in die JSON-Datei
        saveAdvancements();
    }

    private void saveAdvancements() {
        File file = new File(getDataFolder(), ADVANCEMENT_FILE);
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            try (Writer writer = new FileWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(advancements, writer);
            }
        } catch (IOException e) {
            getLogger().severe("Failed to save advancements: " + e.getMessage());
        }
    }

    private void loadCustomAdvancements() {
        File file = new File(getDataFolder(), ADVANCEMENT_FILE);
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            advancements = new Gson().fromJson(reader, HashMap.class);
        } catch (IOException e) {
            getLogger().severe("Failed to load custom advancements: " + e.getMessage());
        }
    }

    private String getAdvancementDisplayName(Advancement adv) {
        if (adv.getDisplay() == null || adv.getDisplay().getTitle() == null) {
            return adv.getKey().getKey();
        }
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', adv.getDisplay().getTitle()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("advreload")) {
            loadCustomAdvancements();
            sender.sendMessage(ChatColor.GREEN + "Advancements and custom messages reloaded.");
            return true;
        }
        return false;
    }
}
