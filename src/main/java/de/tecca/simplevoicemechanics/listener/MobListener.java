package de.tecca.simplevoicemechanics.listener;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.event.VoiceDetectedEvent;
import de.tecca.simplevoicemechanics.service.VoiceAcoustics;
import de.tecca.simplevoicemechanics.service.mob.MobVoiceService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Bukkit entrypoint for mob reactions to voice detection.
 */
public class MobListener implements Listener {

    private final SimpleVoiceMechanics plugin;
    private final MobVoiceService mobVoiceService;

    public MobListener(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
        this.mobVoiceService = new MobVoiceService(plugin);
        startCleanupTask();
    }

    /**
     * Handles voice detection events for mob mechanics.
     */
    @EventHandler
    public void onVoiceDetected(VoiceDetectedEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!plugin.getConfigManager().isMobHearingEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        if (!mobVoiceService.isValidGameMode(player)) {
            return;
        }

        Location loc = event.getLocation();
        double decibels = event.getDecibels();
        boolean isSneaking = player.isSneaking();

        double modifiedDecibels = VoiceAcoustics.applyGlobalModifiers(player, decibels, plugin.getConfigManager());

        if (plugin.getConfigManager().isEnvironmentalLoggingEnabled()) {
            plugin.getLogger().info(String.format(
                    "Environmental modifiers | Original: %.1f dB | Modified: %.1f dB",
                    decibels, modifiedDecibels
            ));
        }

        mobVoiceService.processNearbyMobs(player, loc, isSneaking, modifiedDecibels);
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, mobVoiceService::cleanupTick, 20L, 20L);
    }
}
