package de.tecca.simplevoicemechanics.command;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles all plugin commands for administration.
 *
 * <p>Provides commands for:
 * <ul>
 *   <li>Reloading configuration</li>
 *   <li>Toggling features (hostile, neutral, peaceful, warden, sculk)</li>
 *   <li>Viewing plugin status with ranges</li>
 * </ul>
 *
 * @author Tecca
 * @version 1.0.0
 */
public class VoiceCommand implements CommandExecutor {

    private final SimpleVoiceMechanics plugin;

    public VoiceCommand(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("voicelistener.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;

            case "toggle":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /voicelistener toggle <hostile|neutral|peaceful|warden|sculk>");
                    return true;
                }
                handleToggle(sender, args[1]);
                break;

            case "status":
                handleStatus(sender);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    /**
     * Sends the help message with all available commands.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SimpleVoiceMechanics ===");
        sender.sendMessage(ChatColor.YELLOW + "/voicelistener reload " +
                ChatColor.GRAY + "- Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/voicelistener toggle <category> " +
                ChatColor.GRAY + "- Toggle mob category or sculk");
        sender.sendMessage(ChatColor.YELLOW + "/voicelistener status " +
                ChatColor.GRAY + "- Display current status");
        sender.sendMessage(ChatColor.GRAY + "Categories: hostile, neutral, peaceful, warden, sculk");
    }

    /**
     * Handles the reload command.
     */
    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
    }

    /**
     * Handles the toggle command for features.
     */
    private void handleToggle(CommandSender sender, String feature) {
        ConfigManager config = plugin.getConfigManager();

        switch (feature.toLowerCase()) {
            case "hostile":
                toggleCategory(sender, "Hostile Mobs",
                        config.isHostileMobsEnabled(),
                        "mob-hearing.hostile-mobs.enabled");
                break;

            case "neutral":
                toggleCategory(sender, "Neutral Mobs",
                        config.isNeutralMobsEnabled(),
                        "mob-hearing.neutral-mobs.enabled");
                break;

            case "peaceful":
                toggleCategory(sender, "Peaceful Mobs",
                        config.isPeacefulMobsEnabled(),
                        "mob-hearing.peaceful-mobs.enabled");
                break;

            case "warden":
                toggleCategory(sender, "Warden",
                        config.isWardenEnabled(),
                        "mob-hearing.warden.enabled");
                break;

            case "sculk":
                boolean newSculkState = !config.isSculkEnabled();
                config.setSculkHearingEnabled(newSculkState);
                sender.sendMessage(ChatColor.GREEN + "Sculk Sensors: " +
                        (newSculkState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown category: " + feature);
                sender.sendMessage(ChatColor.GRAY + "Available: hostile, neutral, peaceful, warden, sculk");
                break;
        }
    }

    /**
     * Toggles a mob category.
     */
    private void toggleCategory(CommandSender sender, String name, boolean currentState, String configPath) {
        boolean newState = !currentState;
        plugin.getConfig().set(configPath, newState);
        plugin.saveConfig();
        plugin.getConfigManager().reload();

        sender.sendMessage(ChatColor.GREEN + name + ": " +
                (newState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
    }

    /**
     * Handles the status command, displaying current plugin configuration.
     */
    private void handleStatus(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();

        sender.sendMessage(ChatColor.GOLD + "=== SimpleVoiceMechanics Status ===");
        sender.sendMessage("");

        // Global settings
        sender.sendMessage(ChatColor.YELLOW + "Global Detection:");
        sender.sendMessage(formatRange("  Range",
                config.getDefaultMinRange(), config.getDefaultMaxRange()));
        sender.sendMessage(formatValue("  Falloff Curve", config.getDefaultFalloffCurve()));
        sender.sendMessage("");

        // Mob categories
        sender.sendMessage(ChatColor.YELLOW + "Mob Hearing: " +
                formatEnabled(config.isMobHearingEnabled()));

        if (config.isMobHearingEnabled()) {
            sender.sendMessage(formatCategory("  Hostile Mobs",
                    config.isHostileMobsEnabled(),
                    config.getHostileMinRange(),
                    config.getHostileMaxRange(),
                    config.getHostileFalloffCurve()));

            sender.sendMessage(formatCategory("  Neutral Mobs",
                    config.isNeutralMobsEnabled(),
                    config.getNeutralMinRange(),
                    config.getNeutralMaxRange(),
                    config.getNeutralFalloffCurve()));

            sender.sendMessage(formatCategory("  Peaceful Mobs",
                    config.isPeacefulMobsEnabled(),
                    config.getPeacefulMinRange(),
                    config.getPeacefulMaxRange(),
                    config.getPeacefulFalloffCurve()));

            if (config.isPeacefulMobsEnabled() && config.isFollowWhenSneakingEnabled()) {
                sender.sendMessage(ChatColor.GRAY + "    → Follow when sneaking: " +
                        ChatColor.WHITE + config.getFollowDuration() + "s (" +
                        config.getFollowMaxDistance() + " blocks)");
            }

            sender.sendMessage(formatCategory("  Warden",
                    config.isWardenEnabled(),
                    config.getWardenMinRange(),
                    config.getWardenMaxRange(),
                    config.getWardenFalloffCurve()));
        }

        sender.sendMessage("");

        // Sculk sensors
        sender.sendMessage(formatCategory("Sculk Sensors",
                config.isSculkEnabled(),
                config.getSculkMinRange(),
                config.getSculkMaxRange(),
                config.getSculkFalloffCurve()));

        if (config.isSculkEnabled()) {
            sender.sendMessage(ChatColor.GRAY + "  Cooldown: " +
                    ChatColor.WHITE + config.getSculkCooldown() + "ms");
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Developer API: " + formatApiStatus());
        sender.sendMessage(ChatColor.YELLOW + "Debug Logging: " + formatDebugStatus(config));
    }

    /**
     * Formats public API service status.
     */
    private String formatApiStatus() {
        if (plugin.getApi() == null) {
            return ChatColor.RED + "Unavailable";
        }

        int detectionHooks = plugin.getApi().getDetectionHookCount();
        int mobHooks = plugin.getApi().getMobReactionHookCount();
        int sculkHooks = plugin.getApi().getSculkActivationHookCount();
        return ChatColor.GREEN + "Enabled" + ChatColor.GRAY + " (" +
                ChatColor.WHITE + detectionHooks + ChatColor.GRAY + " detection, " +
                ChatColor.WHITE + mobHooks + ChatColor.GRAY + " mob, " +
                ChatColor.WHITE + sculkHooks + ChatColor.GRAY + " sculk hooks)";
    }

    /**
     * Formats active debug logging flags.
     */
    private String formatDebugStatus(ConfigManager config) {
        StringBuilder active = new StringBuilder();
        appendDebugFlag(active, config.isAudioLoggingEnabled(), "audio");
        appendDebugFlag(active, config.isRangeLoggingEnabled(), "range");
        appendDebugFlag(active, config.isDetectionLoggingEnabled(), "detection");
        appendDebugFlag(active, config.isWardenLoggingEnabled(), "warden");
        appendDebugFlag(active, config.isSculkLoggingEnabled(), "sculk");
        appendDebugFlag(active, config.isPeacefulLoggingEnabled(), "peaceful");
        appendDebugFlag(active, config.isEnvironmentalLoggingEnabled(), "environmental");
        appendDebugFlag(active, config.isGroupAlertLoggingEnabled(), "group-alert");

        if (active.length() == 0) {
            return ChatColor.RED + "none enabled";
        }
        return ChatColor.GREEN + active.toString();
    }

    private void appendDebugFlag(StringBuilder builder, boolean enabled, String name) {
        if (!enabled) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(ChatColor.GRAY).append(", ").append(ChatColor.GREEN);
        }
        builder.append(name);
    }

    /**
     * Formats a status line with enabled/disabled indicator.
     */
    private String formatEnabled(boolean enabled) {
        return enabled ? ChatColor.GREEN + "✓ Enabled" : ChatColor.RED + "✗ Disabled";
    }

    /**
     * Formats a category status line.
     */
    private String formatCategory(String name, boolean enabled, double minRange,
                                  double maxRange, double falloff) {
        String status = enabled ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗";
        String range = String.format("%.1f-%.1f blocks (curve: %.1f)", minRange, maxRange, falloff);
        return status + " " + ChatColor.YELLOW + name + ChatColor.GRAY + ": " +
                ChatColor.WHITE + (enabled ? range : "disabled");
    }

    /**
     * Formats a range display.
     */
    private String formatRange(String name, double min, double max) {
        return ChatColor.GRAY + name + ": " + ChatColor.WHITE +
                String.format("%.1f-%.1f blocks", min, max);
    }

    /**
     * Formats a single value display.
     */
    private String formatValue(String name, double value) {
        return ChatColor.GRAY + name + ": " + ChatColor.WHITE + String.format("%.1f", value);
    }
}
