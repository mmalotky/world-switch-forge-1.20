package com.mmalotky.worldswitch;

import com.mmalotky.worldswitch.IO.IOMethods;
import com.mmalotky.worldswitch.commands.WorldCommand;
import com.mojang.logging.LogUtils;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(WorldSwitch.MOD_ID)
public class WorldSwitch
{
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "com/mmalotky/worldswitch";

    public WorldSwitch() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");

        LOGGER.info("Checking for WorldSet");
        File worldConfig = new File("./worldConfig.cfg");
        if(!WorldCommand.checkWorldConfig(worldConfig)) return;
        String worldName;

        try(
                BufferedReader reader = new BufferedReader(new FileReader(worldConfig));
        ) {
            worldName = reader.readLine();
            PrintWriter writer = new PrintWriter(worldConfig);
            writer.println();
            writer.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
            return;
        }

        File[] worlds = WorldCommand.getWorldsFiles(new File("./worlds"));
        File worldFile = Arrays.stream(worlds).filter(file -> file.getName().equals(worldName)).findFirst().orElse(null);
        if(worldFile == null) {
            LOGGER.error(String.format("World File %s not found", worldName));
            return;
        }

        LOGGER.info(String.format("Setting %s as world", worldName));
        DedicatedServerSettings dedicatedserversettings = new DedicatedServerSettings(Paths.get("server.properties"));
        String level = dedicatedserversettings.getProperties().levelName;
        Path worldPath = Paths.get(level);
        if(Files.exists(worldPath)) IOMethods.deleteDirectory(new File(worldPath.toUri()));
        try {
            Files.createSymbolicLink(worldPath, worldFile.toPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
