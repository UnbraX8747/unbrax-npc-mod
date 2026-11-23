package com.unbrax.afknpc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

import java.util.UUID;

/**
 * /player <isim>                       bot oluşturur
 * /player <isim> kill                  botu kaldırır
 * /player <isim> hotbar <1-9>          botun hotbar slotunu seçer
 * /player <isim> drop <yer>            tek eşya atar
 * /player <isim> stackdrop <yer>       tüm stack'i atar
 * /player <isim> invdrop               tüm envanteri atar
 *
 * <yer>: mainhand, offhand, helmet, chestplate, leggings, boots, slot <1-36>
 * Hile (cheats/op) gerekmez.
 */
public final class BotCommand {
    private BotCommand() {
    }

    private static final SuggestionProvider<CommandSourceStack> BOT_NAMES = (ctx, builder) -> {
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            if (p instanceof BotPlayer) {
                builder.suggest(p.getGameProfile().name());
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("player")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(BOT_NAMES)
                        .executes(BotCommand::spawn)
                        .then(Commands.literal("spawn").executes(BotCommand::spawn))
                        .then(Commands.literal("kill").executes(BotCommand::kill))
                        .then(Commands.literal("hotbar")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                                        .executes(BotCommand::hotbar)))
                        .then(Commands.literal("invdrop").executes(BotCommand::invDrop))
                        .then(dropTree("drop", false))
                        .then(dropTree("stackdrop", true))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dropTree(String literal, boolean wholeStack) {
        return Commands.literal(literal)
                .then(Commands.literal("mainhand").executes(c -> dropEquipment(c, EquipmentSlot.MAINHAND, wholeStack)))
                .then(Commands.literal("offhand").executes(c -> dropEquipment(c, EquipmentSlot.OFFHAND, wholeStack)))
                .then(Commands.literal("helmet").executes(c -> dropEquipment(c, EquipmentSlot.HEAD, wholeStack)))
                .then(Commands.literal("chestplate").executes(c -> dropEquipment(c, EquipmentSlot.CHEST, wholeStack)))
                .then(Commands.literal("leggings").executes(c -> dropEquipment(c, EquipmentSlot.LEGS, wholeStack)))
                .then(Commands.literal("boots").executes(c -> dropEquipment(c, EquipmentSlot.FEET, wholeStack)))
                .then(Commands.literal("slot")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 36))
                                .executes(c -> dropSlot(c, IntegerArgumentType.getInteger(c, "slot") - 1, wholeStack))));
    }

    // ---------- komutlar ----------

    private static int spawn(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        if (name.length() > 16) {
            src.sendFailure(Component.literal("Bot ismi en fazla 16 karakter olabilir."));
            return 0;
        }
        if (src.getServer().getPlayerList().getPlayerByName(name) != null) {
            src.sendFailure(Component.literal(name + " zaten oyunda."));
            return 0;
        }

        Vec2 rot = src.getRotation();
        BotPlayer.spawn(src.getServer(), src.getLevel(), name, src.getPosition(), rot.y, rot.x);
        src.sendSuccess(() -> Component.literal(name + " oluşturuldu."), false);
        return 1;
    }

    private static int kill(CommandContext<CommandSourceStack> ctx) {
        BotPlayer bot = findBot(ctx);
        if (bot == null) {
            return 0;
        }
        bot.remove("Bot kaldırıldı");
        ctx.getSource().sendSuccess(() -> Component.literal("Bot kaldırıldı."), false);
        return 1;
    }

    private static int hotbar(CommandContext<CommandSourceStack> ctx) {
        BotPlayer bot = findBot(ctx);
        if (bot == null) {
            return 0;
        }
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        bot.getInventory().setSelectedSlot(slot - 1);
        ctx.getSource().sendSuccess(() -> Component.literal("Hotbar slotu " + slot + " seçildi."), false);
        return 1;
    }

    private static int dropEquipment(CommandContext<CommandSourceStack> ctx, EquipmentSlot slot, boolean wholeStack) {
        BotPlayer bot = findBot(ctx);
        if (bot == null) {
            return 0;
        }
        ItemStack current = bot.getItemBySlot(slot);
        if (current.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Orada eşya yok."));
            return 0;
        }

        ItemStack toDrop;
        if (wholeStack) {
            toDrop = current.copy();
            bot.setItemSlot(slot, ItemStack.EMPTY);
        } else {
            toDrop = current.split(1);
            if (current.isEmpty()) {
                bot.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        dropItem(bot, ctx.getSource(), toDrop);
        return 1;
    }

    private static int dropSlot(CommandContext<CommandSourceStack> ctx, int index, boolean wholeStack) {
        BotPlayer bot = findBot(ctx);
        if (bot == null) {
            return 0;
        }
        Inventory inv = bot.getInventory();
        ItemStack current = inv.getItem(index);
        if (current.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Orada eşya yok."));
            return 0;
        }

        ItemStack toDrop;
        if (wholeStack) {
            toDrop = current.copy();
            inv.setItem(index, ItemStack.EMPTY);
        } else {
            toDrop = current.split(1);
            if (current.isEmpty()) {
                inv.setItem(index, ItemStack.EMPTY);
            }
        }
        dropItem(bot, ctx.getSource(), toDrop);
        return 1;
    }

    private static int invDrop(CommandContext<CommandSourceStack> ctx) {
        BotPlayer bot = findBot(ctx);
        if (bot == null) {
            return 0;
        }
        Inventory inv = bot.getInventory();
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack toDrop = stack.copy();
            inv.setItem(i, ItemStack.EMPTY);
            dropItem(bot, ctx.getSource(), toDrop);
            count++;
        }
        int dropped = count;
        ctx.getSource().sendSuccess(() -> Component.literal("Envanterden " + dropped + " stack atıldı."), false);
        return count;
    }

    // ---------- yardımcılar ----------

    private static void dropItem(BotPlayer bot, CommandSourceStack src, ItemStack stack) {
        ItemEntity entity = bot.drop(stack, false, false);
        if (entity != null && src.getEntity() instanceof ServerPlayer owner) {
            // Bot kendi attığını geri toplamasın, sadece komutu yazan oyuncu alabilsin.
            UUID id = owner.getUUID();
            entity.setTarget(id);
        }
    }

    private static BotPlayer findBot(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        ServerPlayer player = src.getServer().getPlayerList().getPlayerByName(name);
        if (player instanceof BotPlayer bot) {
            return bot;
        }
        src.sendFailure(Component.literal(name + " adında bir bot yok. Önce /player " + name + " yaz."));
        return null;
    }
}
