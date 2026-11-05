package com.example.wavesurvival.client;

import com.example.wavesurvival.ModEntities;
import com.example.wavesurvival.WaveSurvival;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class ClientSetup {
    public static final KeyMapping SHOP_KEY = new KeyMapping("key.wavesurvival.shop",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.wavesurvival");

    @Mod.EventBusSubscriber(modid = WaveSurvival.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModEntities.WAVE_MOB.get(), WaveMobRenderer::new);
        }
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.register(SHOP_KEY); }
    }

    @Mod.EventBusSubscriber(modid = WaveSurvival.MODID, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            while (SHOP_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null) mc.setScreen(new UpgradeScreen());
            }
        }
    }
}
