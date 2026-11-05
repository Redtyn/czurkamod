package com.example.wavesurvival.economy;

import com.example.wavesurvival.entity.WaveMobEntity;
import com.example.wavesurvival.net.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class Economy {
    public static final int DMG = 0, KNOCKBACK = 1, ATK_SPEED = 2, BUFF_REGEN = 10, BUFF_SPEED = 11, BUFF_RESIST = 12, HEAL = 13;
    private static final UUID ATK_ID = UUID.fromString("b2d0f6aa-0001-4e11-9a00-000000000001");
    private static final int MAX_LEVEL = 10;

    // ---- данные в persistentData игрока ----
    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }
    public static int get(Player p, String k) { return data(p).getInt(k); }
    public static void set(Player p, String k, int v) { data(p).putInt(k, v); }
    public static void addPoints(ServerPlayer p, int n) { set(p, "ws_points", get(p, "ws_points") + n); sync(p); }

    public static void sync(ServerPlayer p) {
        ModNetwork.sendSync(p, get(p, "ws_points"), get(p, "ws_dmg"), get(p, "ws_kb"), get(p, "ws_spd"));
    }

    public static int upgradeCost(int level) { return 50 + level * 40; }

    /** Покупка. Вызывается на сервере из пакета. */
    public static void purchase(ServerPlayer p, int id) {
        int points = get(p, "ws_points");
        switch (id) {
            case DMG, KNOCKBACK, ATK_SPEED -> {
                String key = id == DMG ? "ws_dmg" : id == KNOCKBACK ? "ws_kb" : "ws_spd";
                int lvl = get(p, key);
                int cost = upgradeCost(lvl);
                if (lvl >= MAX_LEVEL || points < cost) return;
                set(p, key, lvl + 1); set(p, "ws_points", points - cost);
                applyAttackSpeed(p);
            }
            case BUFF_REGEN  -> buff(p, 80,  new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 1));
            case BUFF_SPEED  -> buff(p, 60,  new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 45, 1));
            case BUFF_RESIST -> buff(p, 120, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 1));
            case HEAL -> { if (points >= 40) { set(p, "ws_points", points - 40); p.setHealth(p.getMaxHealth()); } }
        }
        sync(p);
    }

    private static void buff(ServerPlayer p, int cost, MobEffectInstance eff) {
        int pts = get(p, "ws_points");
        if (pts < cost) return;
        set(p, "ws_points", pts - cost);
        p.addEffect(eff);
    }

    public static void applyAttackSpeed(Player p) {
        var inst = p.getAttribute(Attributes.ATTACK_SPEED);
        if (inst == null) return;
        inst.removeModifier(ATK_ID);
        int lvl = get(p, "ws_spd");
        if (lvl > 0) inst.addPermanentModifier(new AttributeModifier(ATK_ID, "ws_atk_speed", 0.12 * lvl,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    // ---- события ----
    @SubscribeEvent public void onKill(LivingDeathEvent e) {
        if (e.getEntity() instanceof WaveMobEntity mob && e.getSource().getEntity() instanceof ServerPlayer p) {
            int reward = mob.isBossMob() ? 100 + mob.getWave() * 10 : 8 + mob.getWave() * 2;
            addPoints(p, reward);
            p.displayClientMessage(Component.literal("+" + reward + " очков"), true);
        }
    }

    @SubscribeEvent public void onHurt(LivingHurtEvent e) {
        if (e.getSource().getEntity() instanceof Player p && e.getEntity() instanceof WaveMobEntity) {
            e.setAmount(e.getAmount() * (1f + 0.10f * get(p, "ws_dmg")));
        }
    }

    @SubscribeEvent public void onKnockback(LivingKnockBackEvent e) {
        if (e.getOriginalAttacker() instanceof Player p && e.getEntity() instanceof WaveMobEntity)
            e.setStrength(e.getStrength() * (1f + 0.20f * get(p, "ws_kb")));
    }

    @SubscribeEvent public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { applyAttackSpeed(p); sync(p); }
    }

    @SubscribeEvent public void onClone(PlayerEvent.Clone e) {
        // сохраняем прогресс после смерти
        e.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG,
                e.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy());
    }

    @SubscribeEvent public void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { applyAttackSpeed(p); sync(p); }
    }
}
