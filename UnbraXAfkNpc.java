package com.unbrax.afknpc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class UnbraXAfkNpc implements ModInitializer {
    public static final String MOD_ID = "unbrax_afk_npc";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                BotCommand.register(dispatcher));
    }
}
