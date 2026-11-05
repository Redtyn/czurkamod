package com.example.wavesurvival.client;

import com.example.wavesurvival.economy.Economy;
import com.example.wavesurvival.net.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UpgradeScreen extends Screen {
    private static int points, dmg, kb, spd;

    public UpgradeScreen() { super(Component.literal("Магазин арены")); }

    public static void updateData(int[] a) { points = a[0]; dmg = a[1]; kb = a[2]; spd = a[3]; }

    @Override protected void init() {
        int x = width / 2 - 110, y = height / 2 - 70;
        add(x, y,       "Урон (" + Economy.upgradeCost(dmg) + ")",           Economy.DMG);
        add(x, y + 24,  "Отбрасывание (" + Economy.upgradeCost(kb) + ")",    Economy.KNOCKBACK);
        add(x, y + 48,  "Скорость атаки (" + Economy.upgradeCost(spd) + ")", Economy.ATK_SPEED);
        add(x, y + 80,  "Регенерация (80)",     Economy.BUFF_REGEN);
        add(x, y + 104, "Скорость (60)",        Economy.BUFF_SPEED);
        add(x, y + 128, "Сопротивление (120)",  Economy.BUFF_RESIST);
        add(x, y + 152, "Полное лечение (40)",  Economy.HEAL);
    }

    private void add(int x, int y, String label, int id) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            ModNetwork.sendPurchase(id);
            // перерисовать подписи с новой ценой после ответа сервера
        }).bounds(x, y, 220, 20).build());
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, "Очки: " + points, width / 2, height / 2 - 100, 0xFFD700);
        g.drawCenteredString(font, "Урон ур." + dmg + "  |  Отбр. ур." + kb + "  |  Скор. ур." + spd,
                width / 2, height / 2 - 86, 0xFFFFFF);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }
}
