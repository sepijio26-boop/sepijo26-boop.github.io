package com.autojumpreset.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * هر تیک (بعد از انجام تیک اصلی بازی)، تنظیم Auto Jump رو چک می‌کنه؛
 * اگه روشن شده باشه فوراً خاموشش می‌کنه (ریست به OFF) و یه پیام کوتاه توی اکشن بار نشون می‌ده.
 *
 * روش خاموش کردن مود: کافیه jar رو از پوشه‌ی mods برداری.
 */
@Mixin(MinecraftClient.class)
public class AutoJumpResetMixin {

    /** فاصله‌ی حداقل بین دو پیام، تا اسپم نشه (میلی‌ثانیه) */
    private static final long MESSAGE_COOLDOWN_MS = 5000L;

    private long autojumpreset$lastResetMessageAt = 0L;

    @Inject(method = "tick", at = @At("TAIL"))
    private void autojumpreset$onTick(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client == null || client.options == null) {
            return;
        }

        SimpleOption<Boolean> autoJump = client.options.autoJump;
        if (autoJump == null) {
            return;
        }

        if (Boolean.TRUE.equals(autoJump.getValue())) {
            autoJump.setValue(false);

            long now = System.currentTimeMillis();
            if (now - autojumpreset$lastResetMessageAt > MESSAGE_COOLDOWN_MS) {
                autojumpreset$lastResetMessageAt = now;
                if (client.player != null) {
                    // sendMessage(text, overlay=true) -> پیام توی اکشن بار (بالای هات‌بار)
                    client.player.sendMessage(Text.literal("Auto Jump was reset to OFF"), true);
                }
            }
        }
    }
}
