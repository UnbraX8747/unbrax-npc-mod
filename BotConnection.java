package com.unbrax.afknpc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;

import java.lang.reflect.Field;

/**
 * Gerçek bir istemci olmayan sahte bağlantı. Giden paketler atılır, bu yüzden
 * bellekte birikmez.
 */
public class BotConnection extends Connection {

    public BotConnection() {
        super(PacketFlow.SERVERBOUND);
        installChannel();
    }

    private void installChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                ReferenceCountUtil.release(msg);
                promise.setSuccess();
            }
        });
        try {
            // Alan adı sürüme göre değişebildiği için tipine göre buluyoruz.
            for (Field f : Connection.class.getDeclaredFields()) {
                if (f.getType() == Channel.class) {
                    f.setAccessible(true);
                    f.set(this, channel);
                    return;
                }
            }
            throw new IllegalStateException("Connection içinde Channel alanı bulunamadı");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Bot bağlantısı kurulamadı", e);
        }
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener packetListener) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {
    }
}
