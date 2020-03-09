package com.github.tosindo.signguiapi.listeners;

import org.bukkit.entity.Player;

public interface SignGUIListener {
    void onSignDone(Player player, String[] lines);
}
