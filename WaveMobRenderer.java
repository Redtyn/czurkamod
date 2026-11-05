package com.example.wavesurvival.client;

import com.example.wavesurvival.WaveSurvival;
import com.example.wavesurvival.entity.WaveMobEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class WaveMobRenderer extends HumanoidMobRenderer<WaveMobEntity, HumanoidModel<WaveMobEntity>> {

    private static final ResourceLocation[] SKINS = new ResourceLocation[WaveMobEntity.SKIN_COUNT];
    static {
        for (int i = 0; i < SKINS.length; i++)
            SKINS[i] = new ResourceLocation(WaveSurvival.MODID, "textures/entity/skin_" + (i + 1) + ".png");
    }

    public WaveMobRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(WaveMobEntity mob) {
        return SKINS[Math.floorMod(mob.getSkin(), SKINS.length)];
    }

    @Override
    protected void scale(WaveMobEntity mob, PoseStack pose, float partialTick) {
        float s = mob.isBossMob() ? 1.3f : 1.0f;
        pose.scale(s, s, s);
    }
}
