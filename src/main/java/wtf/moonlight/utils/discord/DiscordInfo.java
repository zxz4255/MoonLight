/*
 * MoonLight Hacked Client
 * Discord RPC — 安卓上完全跳过（无原生库，且原逻辑 userId==null 会 System.exit）
 */
package wtf.moonlight.utils.discord;

import lombok.Getter;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiSelectWorld;
import wtf.moonlight.features.modules.Module;
import wtf.moonlight.features.modules.impl.visual.Interface;
import wtf.moonlight.utils.InstanceAccess;
import wtf.moonlight.utils.android.AndroidCompat;
import wtf.moonlight.utils.concurrent.Workers;
import wtf.moonlight.utils.misc.ServerUtils;

public class DiscordInfo implements InstanceAccess {
    private boolean running = true;
    private long timeElapsed = 0;
    @Getter
    private String name = "Player";
    @Getter
    private String id = "";
    @Getter
    private String smallImageText = "";

    public int getTotal() {
        return INSTANCE.getModuleManager().getModules().size();
    }

    public long getCount() {
        return INSTANCE.getModuleManager().getModules().stream().filter(Module::isEnabled).count();
    }

    public void init() {
        // 安卓：不加载 discord-rpc 原生库，也不跑后台线程
        if (AndroidCompat.isAndroid()) {
            name = "Android";
            System.out.println("[Discord] Skipped on Android launcher.");
            return;
        }

        this.timeElapsed = System.currentTimeMillis();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().setReadyEventHandler(discordUser -> {
            System.out.println("[Discord] Connected to user " + discordUser.username + "#" + discordUser.discriminator);
            if (discordUser.userId != null) {
                name = discordUser.username + (discordUser.discriminator.equals("0") ? "" : discordUser.discriminator);
            } else {
                // 原版 System.exit(0) 在安卓/无 Discord 时会直接杀进程 — 已移除
                System.out.println("[Discord] userId null, ignore (no exit).");
                name = "Player";
            }
        }).build();

        try {
            DiscordRPC.discordInitialize("1266031153572479107", handlers, true);
        } catch (Throwable t) {
            System.out.println("[Discord] Initialize failed: " + t.getMessage());
            running = false;
            return;
        }

        Workers.IO.execute(() -> {
            while (running) {
                try {
                    int killed = INSTANCE.getModuleManager().getModule(Interface.class).killed;
                    int win = INSTANCE.getModuleManager().getModule(Interface.class).won;
                    if (mc.thePlayer != null) {
                        if (mc.isSingleplayer()) {
                            update("Ig: " + detectUsername(), "is in SinglePlayer", true);
                            updateSmallImageText(getCount() + "/" + getTotal() + " modules Enabled" + " | " + "Kills: " + killed + " | Wins: " + win);
                        } else if (mc.getCurrentServerData() != null && !(mc.currentScreen instanceof GuiDownloadTerrain)) {
                            update("Ig: " + detectUsername(), "is on " + ServerUtils.getRemoteIp()
                                    + " " + "(" + mc.getCurrentServerData().populationInfo + ")", true);
                            updateSmallImageText(getCount() + "/" + getTotal() + " modules Enabled" + " | " + "Kills: " + killed + " | Wins: " + win);
                        } else {
                            update("Ig: " + detectUsername(), "is loading a Server", true);
                            updateSmallImageText(getCount() + "/" + getTotal() + " modules Enabled" + " | " + "Kills: " + killed + " | Wins: " + win);
                        }
                    } else if (mc.currentScreen instanceof GuiSelectWorld) {
                        update("Ig: " + detectUsername(), "is selecting a world", true);
                        updateSmallImageText(getCount() + "/" + getTotal() + " modules Enabled" + " | " + "Kills: " + killed + " | Wins: " + win);
                    } else if (mc.currentScreen instanceof GuiMultiplayer) {
                        update("Ig: " + detectUsername(), "is selecting a server", true);
                        updateSmallImageText(getCount() + "/" + getTotal() + " modules Enabled" + " | " + "Kills: " + killed + " | Wins: " + win);
                    } else {
                        update("Ig: " + detectUsername(), "is in MainMenu", true);
                        updateSmallImageText(getCount() + "/" + getTotal() + " modules Enabled" + " | " + "Kills: " + killed + " | Wins: " + win);
                    }
                } catch (Throwable ignored) {
                }
                DiscordRPC.discordRunCallbacks();
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        });
    }

    public void update(String firstLine, String secondLine, boolean showTimeElapsed) {
        if (AndroidCompat.isAndroid() || !running) return;
        try {
            DiscordRichPresence.Builder b = new DiscordRichPresence.Builder(secondLine);
            b.setBigImage("logo", "MoonLight");
            b.setDetails(firstLine);
            if (showTimeElapsed) {
                b.setStartTimestamps(timeElapsed);
            }
            b.setSmallImage("icon", smallImageText);
            DiscordRPC.discordUpdatePresence(b.build());
        } catch (Throwable ignored) {
        }
    }

    public void updateSmallImageText(String text) {
        this.smallImageText = text;
    }

    private String detectUsername() {
        try {
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                return mc.getSession().getUsername();
            }
        } catch (Throwable ignored) {
        }
        return name != null ? name : "Player";
    }

    public void stop() {
        running = false;
        if (AndroidCompat.isAndroid()) return;
        try {
            DiscordRPC.discordShutdown();
        } catch (Throwable ignored) {
        }
    }
}
