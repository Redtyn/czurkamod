package com.example.wavesurvival.net;

import com.example.wavesurvival.WaveSurvival;
import com.example.wavesurvival.client.UpgradeScreen;
import com.example.wavesurvival.economy.Economy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.DistExecutor;

public class ModNetwork {
    private static final String V = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WaveSurvival.MODID, "main"), () -> V, V::equals, V::equals);

    public static void register() {
        int id = 0;
        // Клиент -> сервер: покупка
        CH.messageBuilder(Integer.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((i, buf) -> buf.writeInt(i))
                .decoder(buf -> buf.readInt())
                .consumerMainThread((i, ctx) -> {
                    ServerPlayer p = ctx.get().getSender();
                    if (p != null) Economy.purchase(p, i);
                    ctx.get().setPacketHandled(true);
                }).add();

        // Сервер -> клиент: синхронизация очков и уровней
        CH.messageBuilder(int[].class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((a, buf) -> { for (int v : a) buf.writeInt(v); })
                .decoder(buf -> new int[]{buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()})
                .consumerMainThread((a, ctx) -> {
                    DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> UpgradeScreen.updateData(a));
                    ctx.get().setPacketHandled(true);
                }).add();
    }

    public static void sendPurchase(int id) { CH.sendToServer(id); }

    public static void sendSync(ServerPlayer p, int points, int dmg, int kb, int spd) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), new int[]{points, dmg, kb, spd});
    }
}
