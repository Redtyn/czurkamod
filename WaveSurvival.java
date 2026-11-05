package com.example.wavesurvival;

import com.example.wavesurvival.entity.WaveMobEntity;
import com.example.wavesurvival.economy.Economy;
import com.example.wavesurvival.net.ModNetwork;
import com.example.wavesurvival.wave.WaveCommands;
import com.example.wavesurvival.wave.WaveManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.server.ServerStoppingEvent;

@Mod(WaveSurvival.MODID)
public class WaveSurvival {
    public static final String MODID = "wavesurvival";

    public WaveSurvival() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(modBus);
        modBus.addListener(this::attributes);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(ModNetwork::register));
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new Economy());
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.WAVE_MOB.get(), WaveMobEntity.createAttributes().build());
    }

    @SubscribeEvent public void cmds(RegisterCommandsEvent e) { WaveCommands.register(e.getDispatcher()); }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) WaveManager.tick(e.getServer());
    }

    @SubscribeEvent public void stop(ServerStoppingEvent e) { WaveManager.stop(e.getServer(), false); }
}
