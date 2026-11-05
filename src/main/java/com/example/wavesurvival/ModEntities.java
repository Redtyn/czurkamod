package com.example.wavesurvival;

import com.example.wavesurvival.entity.WaveMobEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WaveSurvival.MODID);

    public static final RegistryObject<EntityType<WaveMobEntity>> WAVE_MOB = ENTITIES.register("wave_mob",
            () -> EntityType.Builder.of(WaveMobEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f).clientTrackingRange(10).build("wave_mob"));
}
