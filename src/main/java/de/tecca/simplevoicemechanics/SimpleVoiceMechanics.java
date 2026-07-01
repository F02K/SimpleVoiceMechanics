package de.tecca.simplevoicemechanics;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.tecca.simplevoicemechanics.api.SimpleVoiceMechanicsApi;
import de.tecca.simplevoicemechanics.api.SimpleVoiceMechanicsApiImpl;
import de.tecca.simplevoicemechanics.command.VoiceCommand;
import de.tecca.simplevoicemechanics.handler.VoiceHandler;
import de.tecca.simplevoicemechanics.listener.MobListener;
import de.tecca.simplevoicemechanics.listener.SculkListener;
import de.tecca.simplevoicemechanics.manager.ConfigManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

/**
 * Main plugin class for SimpleVoiceMechanics.
 *
 * <p>Integrates SimpleVoiceChat with Minecraft mechanics using a range-based detection system:
 * <ul>
 *   <li>Hostile mobs attack speaking players</li>
 *   <li>Neutral mobs look at speaking players</li>
 *   <li>Peaceful mobs look at and follow speaking players when sneaking</li>
 *   <li>Warden reacts with distance-based anger</li>
 *   <li>Sculk Sensors detect voice activity</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Range-based detection (min-range, max-range, falloff-curve)</li>
 *   <li>Per-category configuration overrides</li>
 *   <li>Mob blacklists per category</li>
 *   <li>Version-safe mob categorization</li>
 *   <li>Paper API integration (no NMS)</li>
 * </ul>
 *
 * @author Tecca
 * @version 1.0.0
 */
public final class SimpleVoiceMechanics extends JavaPlugin {

    /** Singleton instance of the plugin */
    private static SimpleVoiceMechanics instance;

    /** Handler for SimpleVoiceChat API integration */
    private VoiceHandler voiceHandler;

    /** Manager for plugin configuration */
    private ConfigManager configManager;

    /** Public integration API exposed through Bukkit services */
    private SimpleVoiceMechanicsApiImpl api;

    /**
     * Called when the plugin is enabled.
     *
     * <p>Initializes configuration, registers SimpleVoiceChat integration,
     * sets up event listeners, and registers commands.
     */
    @Override
    public void onEnable() {
        instance = this;

        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Register public API service for other plugins
        api = new SimpleVoiceMechanicsApiImpl(this);
        getServer().getServicesManager().register(SimpleVoiceMechanicsApi.class, api, this, ServicePriority.Normal);

        // Register SimpleVoiceChat service
        if (!initializeVoiceChat()) {
            return;
        }

        // Register event listeners
        registerListeners();

        // Register commands
        registerCommands();

        getLogger().info("SimpleVoiceMechanics successfully enabled!");
        getLogger().info("Range-based detection system active");
        getLogger().info("Paper API integration (no NMS)");
    }

    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (api != null) {
            getServer().getServicesManager().unregister(SimpleVoiceMechanicsApi.class, api);
        }
        getLogger().info("SimpleVoiceMechanics disabled!");
    }

    /**
     * Initializes SimpleVoiceChat integration.
     *
     * @return true if initialization was successful, false otherwise
     */
    private boolean initializeVoiceChat() {
        BukkitVoicechatService service = getServer().getServicesManager()
                .load(BukkitVoicechatService.class);

        if (service == null) {
            getLogger().severe("SimpleVoiceChat plugin not found! Please install it.");
            getLogger().severe("Download: https://modrinth.com/plugin/simple-voice-chat");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        voiceHandler = new VoiceHandler(this);
        service.registerPlugin(voiceHandler);
        getLogger().info("SimpleVoiceChat API successfully integrated!");
        return true;
    }

    /**
     * Registers all event listeners for the plugin.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MobListener(this), this);
        getServer().getPluginManager().registerEvents(new SculkListener(this), this);
        getLogger().info("Event listeners registered (MobListener, SculkListener)");
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        PluginCommand command = getCommand("voicelistener");
        if (command == null) {
            getLogger().severe("Command voicelistener is missing from plugin.yml");
            return;
        }
        command.setExecutor(new VoiceCommand(this));
        getLogger().info("Commands registered (/voicelistener)");
    }

    /**
     * Gets the singleton instance of the plugin.
     *
     * @return the plugin instance
     */
    public static SimpleVoiceMechanics getInstance() {
        return instance;
    }

    /**
     * Gets the voice handler for SimpleVoiceChat integration.
     *
     * @return the voice handler
     */
    public VoiceHandler getVoiceHandler() {
        return voiceHandler;
    }

    /**
     * Gets the configuration manager.
     *
     * @return the config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the public SimpleVoiceMechanics API service implementation.
     *
     * @return the API service implementation
     */
    public SimpleVoiceMechanicsApiImpl getApi() {
        return api;
    }
}
