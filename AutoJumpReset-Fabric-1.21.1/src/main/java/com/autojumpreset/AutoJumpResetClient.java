package com.autojumpreset;

import net.fabricmc.api.ClientModInitializer;

/**
 * Auto Jump Reset — Fabric 1.21.1 (client-side)
 *
 * کار اصلی توی میکسین AutoJumpResetMixin انجام می‌شه؛
 * این کلاس فقط entrypoint استاندارد فابریکه.
 *
 * نکته: این مود هیچ وابستگی به Fabric API نداره؛ فقط خود Fabric Loader کافیه.
 * همین باعث می‌شه توی لانچرهایی که مود Fabric رو ساپورت می‌کنن (مثل Lunar روی نسخه‌های جدید)
 * بدون نیاز به مود اضافه، فقط با همون یه jar نصب بشه.
 */
public class AutoJumpResetClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // همه‌چیز توی میکسین انجام می‌شه
    }
}
