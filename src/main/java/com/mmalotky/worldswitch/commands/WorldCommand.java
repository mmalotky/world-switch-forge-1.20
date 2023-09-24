package com.mmalotky.worldswitch.commands;

import com.mmalotky.worldswitch.IO.IOMethods;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class WorldCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    public WorldCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("world")
                .then(Commands.argument("action", StringArgumentType.word())
                .then(Commands.argument("world", StringArgumentType.word())
                .executes(command -> {
                    String action = StringArgumentType.getString(command, "action");
                    switch (action) {
                        case "set": return setWorld(command.getSource(), StringArgumentType.getString(command, "world"));
                        case "save": return saveWorld(command.getSource(), StringArgumentType.getString(command, "world"));
                        default: LOGGER.info(String.format("%s is not a an action%n", action));
                    }
                    return 0;
                }))));
    }

    private int saveWorld(CommandSourceStack source, String world) {
        File worldsFile = source.getServer().getFile("./worlds");
        if(getWorldsFiles(worldsFile) == null) {
            source.sendFailure(Component.literal("IO Failure"));
            return 0;
        }

        Path destination = Path.of(String.format("%s/%s",worldsFile.getAbsolutePath(),world));
        if(world.equals("new") || Files.exists(destination)) {
            String msg = String.format("%s is not available", world);
            LOGGER.error(msg);
            source.sendFailure(Component.literal(msg));
            return 0;
        }

        String worldName = source.getServer().getWorldData().getLevelName();
        Path origin = source.getServer().getFile(String.format("./%s", worldName)).toPath();
        IOMethods.copyDirectory(origin, destination);

        source.sendSystemMessage(Component.literal(String.format("World '%s' saved", world)));
        return Command.SINGLE_SUCCESS;
    }
    private int setWorld(CommandSourceStack source, String world) {
        File worldsFile = source.getServer().getFile("./worlds");
        File[] worldsFiles = getWorldsFiles(worldsFile);
        if(worldsFiles == null) {
            source.sendFailure(Component.literal("IO Failure"));
            return 0;
        }

        if(!world.equals("new") && Arrays.stream(worldsFiles).noneMatch(file -> file.getName().equals(world))) {
            String msg = String.format("World '%s' not recognised", world);
            LOGGER.error(msg);
            source.sendFailure(Component.literal(msg));
            return 0;
        }

        if(!world.equals("new")) {
            LOGGER.info(String.format("Updating worldConf.cfg for world '%s'", world));
            File worldConfig = new File("./worldConfig.cfg");
            if(!checkWorldConfig(worldConfig)) {
                source.sendFailure(Component.literal("IO Failure"));
                return 0;
            }

            try(PrintWriter writer = new PrintWriter(worldConfig)) {
                writer.println(world);
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                source.sendFailure(Component.literal("IO error"));
                e.printStackTrace();
                return 0;
            }
        }

        LOGGER.info("Disconnecting Players");
        source.getLevel()
                .getPlayers(p -> true)
                .forEach(player -> player.connection.disconnect(Component.literal("Server Shut Down")));

        source.getServer().halt(false);
        return Command.SINGLE_SUCCESS;
    }

    public static File[] getWorldsFiles(File worldsFile) {
        if(!worldsFile.exists() && worldsFile.mkdir()) LOGGER.error("Could not create worlds folder");;
        File[] worldsFiles = worldsFile.listFiles();
        if(worldsFiles == null) {
            LOGGER.error("Worlds file not found.");
        }
        return worldsFiles;
    }

    public static void setupPlayerDataFiles() {
        File playerDataFile = new File("./playerData");
        if(!playerDataFile.exists() && playerDataFile.mkdir()) LOGGER.error("Could not create playerData folder");
    }

    public static boolean checkWorldConfig(File worldConfig) {
        try {
            if(!worldConfig.exists() && !worldConfig.createNewFile()) {
                LOGGER.error("Could not create worldConfig.cfg");
                return false;
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
