package com.github.tosindo.signguiapi.core;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.github.tosindo.signguiapi.SignGUIAPI;
import com.github.tosindo.signguiapi.listeners.SignGUIListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SignGUI {
    private final static SignGUI instance = new SignGUI();
    protected PacketAdapter packetAdapter;

    public enum SignType {
        OAK(Material.OAK_SIGN),
        DARK_OAK(Material.DARK_OAK_SIGN),
        BIRCH(Material.BIRCH_WOOD),
        JUNGLE(Material.JUNGLE_SIGN),
        ACACIA(Material.ACACIA_SIGN),
        SPRUCE(Material.SPRUCE_SIGN);

        private Material material;

        public Material getMaterial() {
            return material;
        }

        SignType(Material material)
        {
            this.material = material;
        }

    }

    protected ProtocolManager protocolManager;
    protected Map<UUID, SignGUIListener> listeners = new ConcurrentHashMap<>();
    protected Map<UUID, Location> locations = new ConcurrentHashMap<>();

    private SignGUI() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();


        protocolManager.addPacketListener(packetAdapter = new PacketAdapter(SignGUIAPI.getPlugin(), ListenerPriority.HIGHEST, PacketType.Play.Client.UPDATE_SIGN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID playerUUID = player.getUniqueId();

                Location playerSignLocation = locations.remove(playerUUID);
                BlockPosition blockPosition = event.getPacket().getBlockPositionModifier().getValues().get(0);
                //WrappedChatComponent[] wrappedLinesArray = event.getPacket().getChatComponentArrays().getValues().get(0);
                //String[] lines = {wrappedLinesArray[0].getJson(), wrappedLinesArray[1].getJson(), wrappedLinesArray[2].getJson(), wrappedLinesArray[3].getJson()};
                String[] lines = event.getPacket().getStringArrays().getValues().get(0);

                SignGUIListener signInputListener = listeners.remove(playerUUID);

                if (playerSignLocation == null) return;
                //if (!blockPosition.toVector().equals(playerSignLocation.toVector())) return;
                if (blockPosition.getX() != playerSignLocation.getBlockX()) return;
                if (blockPosition.getY() != playerSignLocation.getBlockY()) return;
                if (blockPosition.getZ() != playerSignLocation.getBlockZ()) return;
                if (signInputListener != null) {
                    event.setCancelled(true);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(SignGUIAPI.getPlugin(), () -> signInputListener.onSignDone(player, lines));
                }
            }
        });
    }

    public void openSign(Player player, String[] defaultText, SignType signType, SignGUIListener listener) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(listener);
        Objects.requireNonNull(signType);

        List<PacketContainer> packets = new ArrayList<>();
        BlockPosition signLocation = new BlockPosition(player.getLocation().getBlockX(), 0, player.getLocation().getBlockZ());

        player.sendBlockChange(signLocation.toLocation(player.getWorld()), signType.getMaterial().createBlockData());

        if (defaultText != null) {
            PacketContainer signUpdate = protocolManager.createPacket(PacketType.Play.Server.TILE_ENTITY_DATA);

            signUpdate.getBlockPositionModifier().write(0, signLocation);
            signUpdate.getIntegers().write(0, 9);

            NbtCompound signNbt = NbtFactory.ofCompound("InvictaSign");
            signNbt.put(NbtFactory.of("id", "minecraft:sign"));
            signNbt.put(NbtFactory.of("Color", "black"));
            signNbt.put(NbtFactory.of("x", signLocation.getX()));
            signNbt.put(NbtFactory.of("y", signLocation.getY()));
            signNbt.put(NbtFactory.of("z", signLocation.getZ()));

            for (int i = 0; i<4; i++) {
                signNbt.put(NbtFactory.of("Text"+(i+1), (defaultText.length > i) ? String.format("{\"text\":\"%s\"}", defaultText[i]) : ""));
            }

            signUpdate.getNbtModifier().write(0, signNbt);

            packets.add(signUpdate);
        }

        PacketContainer openSignEditor = protocolManager.createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        openSignEditor.getBlockPositionModifier().write(0, signLocation);

        packets.add(openSignEditor);


        try {
            for (PacketContainer packet : packets) {
                Bukkit.getLogger().log(Level.INFO, packet.getType().toString());
                protocolManager.sendServerPacket(player, packet);
            }

            Bukkit.getLogger().log(Level.INFO, "Sign setup worked properly.");
            locations.put(player.getUniqueId(), signLocation.toLocation(player.getWorld()));
            listeners.put(player.getUniqueId(), listener);

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public void destroy() {
        protocolManager.removePacketListener(packetAdapter);
        locations.clear();
        listeners.clear();
    }

    public static SignGUI getInstance() {
        return instance;
    }
}
