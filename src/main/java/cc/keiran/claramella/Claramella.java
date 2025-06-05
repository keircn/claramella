package cc.keiran.claramella;

import org.bukkit.plugin.java.JavaPlugin;
import cc.keiran.claramella.features.sleep.SleepListener;
import cc.keiran.claramella.features.welcome.WelcomeListener;

public class Claramella extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new WelcomeListener(this), this);
        getServer().getPluginManager().registerEvents(new SleepListener(this), this);

        getLogger().info("claramella has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Claramella has been disabled.");
    }
}
