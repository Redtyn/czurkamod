package com.example.wavesurvival.wave;

import com.example.wavesurvival.ModEntities;
import com.example.wavesurvival.economy.Economy;
import com.example.wavesurvival.entity.WaveMobEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class WaveManager {
    public enum State { IDLE, COUNTDOWN, ACTIVE }

    // ===== Настройки =====
    public static final int COUNTDOWN_TICKS = 20 * 15;   // пауза между волнами
    public static final int SPAWN_INTERVAL  = 20;        // 1 моб в секунду
    public static final int MAX_ALIVE       = 25;
    public static final int BOSS_EVERY      = 5;

    public static State state = State.IDLE;
    public static int wave = 0;
    public static final List<BlockPos> spawnPoints = new ArrayList<>();
    public static BlockPos playerPoint = null;

    private static int timer = 0;
    private static int toSpawn = 0;
    private static boolean bossPending = false;
    private static final Set<UUID> alive = new HashSet<>();
    private static final Random RNG = new Random();

    public static void start(MinecraftServer server) {
        if (spawnPoints.isEmpty()) { broadcast(server, "§cНет точек спавна! /wave addspawn"); return; }
        wave = 0; alive.clear();
        if (playerPoint != null)
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                p.teleportTo(playerPoint.getX() + .5, playerPoint.getY(), playerPoint.getZ() + .5);
        state = State.COUNTDOWN; timer = 20 * 5;
        broadcast(server, "§eВолны начнутся через 5 секунд!");
    }

    public static void stop(MinecraftServer server, boolean announce) {
        ServerLevel lvl = server.overworld();
        for (UUID id : alive) { Entity e = lvl.getEntity(id); if (e != null) e.discard(); }
        alive.clear(); state = State.IDLE;
        if (announce) broadcast(server, "§cРежим волн остановлен.");
    }

    public static void tick(MinecraftServer server) {
        if (state == State.IDLE) return;
        ServerLevel level = server.overworld();

        if (server.getPlayerList().getPlayerCount() == 0
                || server.getPlayerList().getPlayers().stream().allMatch(ServerPlayer::isSpectator)) return;

        switch (state) {
            case COUNTDOWN -> {
                if (--timer <= 0) beginWave(server);
                else if (timer % 20 == 0 && timer <= 100)
                    actionbar(server, "Волна " + (wave + 1) + " через " + (timer / 20) + "…");
            }
            case ACTIVE -> {
                alive.removeIf(id -> { Entity e = level.getEntity(id); return e == null || !e.isAlive(); });

                if (toSpawn > 0 && alive.size() < MAX_ALIVE && server.getTickCount() % SPAWN_INTERVAL == 0) {
                    boolean boss = bossPending && toSpawn == 1; // босс — последним в волне
                    spawnMob(level, boss);
                    if (boss) bossPending = false;
                    toSpawn--;
                }
                if (toSpawn == 0 && alive.isEmpty()) endWave(server);
            }
            default -> {}
        }
    }

    private static void beginWave(MinecraftServer server) {
        wave++;
        boolean bossWave = wave % BOSS_EVERY == 0;
        toSpawn = 5 + wave * 3;
        bossPending = bossWave;
        if (bossWave) toSpawn += 1;
        state = State.ACTIVE;
        broadcast(server, bossWave ? "§4§l⚔ ВОЛНА " + wave + " — БОСС! ⚔" : "§6Волна " + wave + " началась! Врагов: " + toSpawn);
    }

    private static void endWave(MinecraftServer server) {
        int bonus = 20 + wave * 5;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Economy.addPoints(p, bonus);
            p.heal(6f);
        }
        broadcast(server, "§aВолна " + wave + " пройдена! Бонус: +" + bonus + " очков. Магазин — клавиша B.");
        state = State.COUNTDOWN; timer = COUNTDOWN_TICKS;
    }

    private static void spawnMob(ServerLevel level, boolean boss) {
        BlockPos sp = spawnPoints.get(RNG.nextInt(spawnPoints.size()));
        WaveMobEntity mob = new WaveMobEntity(ModEntities.WAVE_MOB.get(), level);
        mob.moveTo(sp.getX() + .5 + RNG.nextDouble() - .5, sp.getY(), sp.getZ() + .5 + RNG.nextDouble() - .5,
                RNG.nextFloat() * 360f, 0f);
        mob.applyWave(wave, boss);
        level.addFreshEntity(mob);
        alive.add(mob.getUUID());
    }

    private static void broadcast(MinecraftServer s, String msg) {
        s.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
    }
    private static void actionbar(MinecraftServer s, String msg) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) p.displayClientMessage(Component.literal(msg), true);
    }
}
