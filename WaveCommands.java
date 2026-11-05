package com.example.wavesurvival.wave;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class WaveCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("wave").requires(s -> s.hasPermission(2))
            .then(Commands.literal("start").executes(c -> { WaveManager.start(c.getSource().getServer()); return 1; }))
            .then(Commands.literal("stop").executes(c -> { WaveManager.stop(c.getSource().getServer(), true); return 1; }))
            .then(Commands.literal("addspawn").executes(c -> {
                BlockPos p = BlockPos.containing(c.getSource().getPosition());
                WaveManager.spawnPoints.add(p);
                c.getSource().sendSuccess(() -> Component.literal("Точка спавна #" + WaveManager.spawnPoints.size() + ": " + p.toShortString()), false);
                return 1; }))
            .then(Commands.literal("clearspawns").executes(c -> { WaveManager.spawnPoints.clear(); return 1; }))
            .then(Commands.literal("setplayer").executes(c -> {
                WaveManager.playerPoint = BlockPos.containing(c.getSource().getPosition());
                c.getSource().sendSuccess(() -> Component.literal("Точка игрока задана"), false);
                return 1; }))
        );
    }
}
