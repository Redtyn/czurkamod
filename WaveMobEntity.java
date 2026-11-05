package com.example.wavesurvival.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.UUID;

public class WaveMobEntity extends Monster {
    public static final int SKIN_COUNT = 8;
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(WaveMobEntity.class, EntityDataSerializers.INT);

    // ===== ЗДЕСЬ МЕНЯЮТСЯ НИКИ МОБОВ =====
    private static final String[] NAMES = {
            "Ахмед", "Мага", "Муслим", "Мустафо", "Муххамад", "Абубакар",
            "Рамазан", "Ислам", "Алан", "Халид", "Рахид", "Алихан"
    };
    private static final String[] BOSS_NAMES = {
            "Аллах", "Предвадитель чурок", "Кривоносый", "Главный хуесос чечни Ахмед"
    };
    // =====================================

    private static final UUID HP_ID    = UUID.fromString("a1c0f6aa-0001-4e11-9a00-000000000001");
    private static final UUID DMG_ID   = UUID.fromString("a1c0f6aa-0002-4e11-9a00-000000000002");
    private static final UUID SPD_ID   = UUID.fromString("a1c0f6aa-0003-4e11-9a00-000000000003");
    private static final UUID ARMOR_ID = UUID.fromString("a1c0f6aa-0004-4e11-9a00-000000000004");

    private int wave = 1;
    private boolean boss = false;
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public WaveMobEntity(EntityType<? extends WaveMobEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN, 0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Вызывается WaveManager сразу после создания моба. */
    public void applyWave(int wave, boolean isBoss) {
        this.wave = wave;
        this.boss = isBoss;

        setSkin(random.nextInt(SKIN_COUNT));

        String name = isBoss ? BOSS_NAMES[random.nextInt(BOSS_NAMES.length)]
                             : NAMES[random.nextInt(NAMES.length)];
        setCustomName(Component.literal(isBoss ? "☠ " + name + " ☠" : name));
        setCustomNameVisible(true);

        double w = wave - 1;
        double hp    = 4.0 * w;
        double dmg   = 0.5 * w;
        double spd   = Math.min(0.02 * w, 0.6);
        double armor = Math.min(0.5 * w, 20.0);

        if (isBoss) {
            hp    = 100 + 15.0 * wave;
            dmg   = 4 + 1.0 * wave;
            spd  += 0.15;
            armor = Math.min(8 + wave, 25);
        }

        replace(Attributes.MAX_HEALTH,     HP_ID,    "wave_hp",    hp,    AttributeModifier.Operation.ADDITION);
        replace(Attributes.ATTACK_DAMAGE,  DMG_ID,   "wave_dmg",   dmg,   AttributeModifier.Operation.ADDITION);
        replace(Attributes.MOVEMENT_SPEED, SPD_ID,   "wave_spd",   spd,   AttributeModifier.Operation.MULTIPLY_BASE);
        replace(Attributes.ARMOR,          ARMOR_ID, "wave_armor", armor, AttributeModifier.Operation.ADDITION);
        setHealth(getMaxHealth());

        if (isBoss) {
            bossBar.setName(getCustomName());
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            setGlowingTag(true);
            xpReward = 100;
        } else if (wave >= 5) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(wave >= 10 ? Items.IRON_SWORD : Items.STONE_SWORD));
        }
    }

    private void replace(Attribute attr, UUID id, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    public int getSkin() { return entityData.get(SKIN); }
    public void setSkin(int s) { entityData.set(SKIN, Math.floorMod(s, SKIN_COUNT)); }
    public int getWave() { return wave; }
    public boolean isBossMob() { return boss; }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); if (boss) bossBar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p)  { super.stopSeenByPlayer(p);  bossBar.removePlayer(p); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (boss) bossBar.setProgress(getHealth() / getMaxHealth());
    }

    @Override public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Skin", getSkin()); t.putInt("Wave", wave); t.putBoolean("Boss", boss);
    }
    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setSkin(t.getInt("Skin")); wave = t.getInt("Wave"); boss = t.getBoolean("Boss");
        if (boss && hasCustomName()) bossBar.setName(getCustomName());
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean isSunBurnTick() { return false; }
}
