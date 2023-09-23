package com.mmalotky.worldswitch.events;

import com.mmalotky.worldswitch.WorldSwitch;
import com.mmalotky.worldswitch.commands.WorldCommand;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = WorldSwitch.MOD_ID)
public class ModEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        new WorldCommand(event.getDispatcher());
    }

    @SubscribeEvent
    public static void injectPlayerData(ServerStartingEvent event) {
        String serverDirectory = event.getServer().getServerDirectory().getAbsolutePath();
        String world = event.getServer().getWorldData().getLevelName();

        Path playerData = Path.of(serverDirectory + "/playerData/playerdata");
        Path link = Path.of(serverDirectory + String.format("/%s/playerdata", world));
        if(Files.isSymbolicLink(link)) return;

        LOGGER.info(String.format("Creating a symbolic link at /%s/playerdata", world));
        try {
            if(Files.exists(link))  Files.delete(link);
            Files.createSymbolicLink(link, playerData);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }

        String[] players = new File(String.valueOf(playerData)).list();
        if(players == null || players.length == 0) return;

        double x = event.getServer().getWorldData().overworldData().getXSpawn();
        double y = event.getServer().getWorldData().overworldData().getYSpawn();
        double z = event.getServer().getWorldData().overworldData().getZSpawn();
        LOGGER.info(String.format("Updating spawn to %s, %s, %s", x, y, z));

        File[] playerFiles = new File(String.valueOf(playerData)).listFiles();
        if(playerFiles == null) return;
        for(File file : playerFiles) {
            if(file.getName().contains(".dat_old")) continue;
            LOGGER.info("Updating " + file.getName());

            try(FileInputStream input = new FileInputStream(file)) {
                CompoundTag tag = NbtIo.readCompressed(input);

                ListTag pos = new ListTag();
                pos.add(DoubleTag.valueOf(x));
                pos.add(DoubleTag.valueOf(y));
                pos.add(DoubleTag.valueOf(z));

                tag.put("Pos", pos);
                NbtIo.writeCompressed(tag, file);
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        }

        LOGGER.info("Player Data Set");
    }
}
