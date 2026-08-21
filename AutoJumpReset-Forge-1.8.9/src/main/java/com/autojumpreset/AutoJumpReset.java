package com.autojumpreset;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Auto Jump Reset — Forge 1.8.9
 *
 * هر تیک، تنظیم Auto Jump کلاینت رو چک می‌کنه؛ اگه روشن شده باشه،
 * فوراً خاموشش می‌کنه (ریست به OFF). اگه بخواد دوباره روشن بشه، توی تیک بعدی دوباره خاموش می‌شه.
 *
 * این مود کاملاً کلاینتیه و هیچ چکی روی حساب (پریمیوم/آفلاین) نمی‌کنه؛
 * پس توی هر لانچری — حتی آفلاین/کرک — همون‌طور کار می‌کنه.
 */
@Mod(
        modid = AutoJumpReset.MODID,
        name = "Auto Jump Reset",
        version = "1.0.0",
        acceptedMinecraftVersions = "[1.8.9]"
)
public class AutoJumpReset {

    public static final String MODID = "autojumpreset";

    /** فاصله‌ی حداقل بین دو پیام اطلاع‌رسانی، تا چت اسپم نشه (میلی‌ثانیه) */
    private static final long MESSAGE_COOLDOWN_MS = 5000L;

    private long lastResetMessageAt = 0L;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.gameSettings == null) {
            return;
        }

        if (mc.gameSettings.autoJump) {
            mc.gameSettings.autoJump = false;

            long now = System.currentTimeMillis();
            if (now - lastResetMessageAt > MESSAGE_COOLDOWN_MS) {
                lastResetMessageAt = now;
                mc.thePlayer.addChatMessage(
                        new ChatComponentText("\u00a7e[AutoJumpReset] \u00a77Auto Jump was reset to \u00a7cOFF")
                );
            }
        }
    }
}
