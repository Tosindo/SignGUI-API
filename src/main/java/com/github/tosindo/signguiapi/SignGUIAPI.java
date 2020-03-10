package com.github.tosindo.signguiapi;

import com.github.tosindo.signguiapi.core.SignGUI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public final class SignGUIAPI extends JavaPlugin {

    private static SignGUIAPI currentInstance;


    public static SignGUIAPI getPlugin() {
        return currentInstance;
    }

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null || !Objects.requireNonNull(getServer().getPluginManager().getPlugin("ProtocolLib")).isEnabled()) {
            getLogger().log(Level.SEVERE, "ProtocolLib not found unable to continue.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        currentInstance = this;
    }

    @Override
    public void onDisable() {
        SignGUI.getInstance().destroy();
    }
}
