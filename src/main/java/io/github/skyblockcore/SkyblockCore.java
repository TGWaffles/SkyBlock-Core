/*
 * Copyright 2023 SkyBlock-Core Contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.skyblockcore;

import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.mojang.brigadier.CommandDispatcher;
import io.github.skyblockcore.command.SkyblockCoreCommand;
import io.github.skyblockcore.event.JoinSkyblockCallback;
import io.github.skyblockcore.event.LeaveSkyblockCallback;
import io.github.skyblockcore.event.LocationChangedCallback;
import io.github.skyblockcore.event.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.io.FileWriter;

public class SkyblockCore implements ClientModInitializer {


    public static final String ModID = "skyblockcore";

    private static boolean ON_SKYBLOCK = false;

    public static boolean isOnSkyblock() {
        return ON_SKYBLOCK;
    }

    public static String getLocation() {
        return LOCATION;
    }

    private static String LOCATION;

    public static final Logger LOGGER = LoggerFactory.getLogger(ModID);

    private static ModConfig config;
    private static final String TITLE = "[SkyblockCore]";


    public static void loadConfig() {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("SkyblockCoreConfig.json").toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Gson gson = new GsonBuilder().create();
                config = gson.fromJson(reader, ModConfig.class);
                if (config != null && config.isDev()) {
                    LOGGER.info(TITLE + " Config file loaded. [Dev]");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            createConfig();
            LOGGER.info(TITLE + " Config successfully created! Welcome to SkyblockCore, a Skyblock mod designed for modern Minecraft.");
        }
    }

    public static void createConfig() {
        config = new ModConfig(); // Below toggles will only change every time you wipe a config!
        config.setDev(false); // This will set all development features and logs to "TRUE". this is used for debugging errors with leaving/joining skyblock.
        config.setLocation(false); // this is used for location output (see line 111 - 112) this is used for debugging new locations.
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(config);
        File configFile = new File(FabricLoader.getInstance().getConfigDir() + File.separator + "SkyblockCoreConfig.json");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ModConfig getConfig() {
        return config; // see io.github.skyblockcore/event/ModConfig
    }

    @Override
    public void onInitializeClient() {
        // We Must load the config first. Otherwise, Events relying on the config such as Location do not work.
        loadConfig();
        ClientCommandRegistrationCallback.EVENT.register(SkyblockCore::registerCommands);
        //ModConfig config = SkyblockCore.getConfig();
        // example of events, TODO; move to a better class
        JoinSkyblockCallback.EVENT.register(() -> {
            ON_SKYBLOCK = true;
            return ActionResult.PASS;
        });
        LeaveSkyblockCallback.EVENT.register(() -> {
            ON_SKYBLOCK = false;
            LOCATION = null;
            return ActionResult.PASS;
        });
        LocationChangedCallback.EVENT.register(((oldLocation, newLocation) -> {
            // Simple Logging Statement for mod developers to debug locations affecting their code.
            if (getConfig() != null && getConfig().isLocation()) {
                LOGGER.info(TITLE + " Detected Location Change on Scoreboard! [Dev Old Location] > " + oldLocation);
                LOGGER.info(TITLE + " Detected Location Change on Scoreboard! [Dev New Location] > " + newLocation);
            }
            LOCATION = newLocation;
            return ActionResult.PASS;
        }));

    }

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        SkyblockCoreCommand.register(dispatcher);
    }
}