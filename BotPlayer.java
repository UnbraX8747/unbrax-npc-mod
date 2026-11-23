package com.unbrax.afknpc;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Hareket etmeyen gerçek oyuncu botu. Normal bir oyuncu gibi dünyaya eklenir,
 * bu yüzden etrafındaki chunklar yüklü kalır ve simüle edilir.
 */
public class BotPlayer extends ServerPlayer {
    private final MinecraftServer botServer;

    private BotPlayer(MinecraftServer server, ServerLevel level, GameProfile profile) {
        super(server, level, profile, ClientInformation.createDefault());
        this.botServer = server;
    }

    public static BotPlayer spawn(MinecraftServer server, ServerLevel level, String name,
                                  Vec3 pos, float yaw, float pitch) {
        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        BotPlayer bot = new BotPlayer(server, level, profile);

        server.getPlayerList().placeNewPlayer(
                new BotConnection(),
                bot,
                new CommonListenerCookie(profile, 0, bot.clientInformation(), false));

        bot.teleportTo(level, pos.x, pos.y, pos.z, Set.<Relative>of(), yaw, pitch, true);
        return bot;
    }

    @Override
    public void tick() {
        // Chunk takibini canlı tut: etraftaki chunklar yüklü ve aktif kalır.
        if (botServer.getTickCount() % 10 == 0) {
            ((ServerLevel) this.level()).getChunkSource().move(this);
        }
        super.tick();
        this.doTick();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // Ölen bot tekrar doğamaz, listeden çıkar.
        remove("Bot öldü");
    }

    public void remove(String reason) {
        botServer.schedule(new TickTask(botServer.getTickCount(), () ->
                this.connection.onDisconnect(new DisconnectionDetails(Component.literal(reason)))));
    }
}
