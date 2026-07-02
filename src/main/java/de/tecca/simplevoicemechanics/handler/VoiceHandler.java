package de.tecca.simplevoicemechanics.handler;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import de.tecca.simplevoicemechanics.api.VoiceDetectionContext;
import de.tecca.simplevoicemechanics.event.VoiceDetectedEvent;
import de.tecca.simplevoicemechanics.util.AudioUtils;
import de.tecca.simplevoicemechanics.util.PluginPermissions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles SimpleVoiceChat API integration and voice packet processing.
 *
 * <p>This handler:
 * <ul>
 *   <li>Registers with SimpleVoiceChat API</li>
 *   <li>Decodes Opus audio packets in real-time</li>
 *   <li>Calculates audio levels in decibels</li>
 *   <li>Fires VoiceDetectedEvent for audio above threshold</li>
 * </ul>
 *
 * @author Tecca
 * @version 1.2.0
 */
public class VoiceHandler implements VoicechatPlugin {

    /** Plugin ID for SimpleVoiceChat registration */
    private static final String PLUGIN_ID = "simplevoicemechanics";
    private static final double VOICE_SOURCE_Y_OFFSET = 1.0;

    private final SimpleVoiceMechanics plugin;
    private VoicechatApi voicechatApi;
    private final ThreadLocal<OpusDecoder> decoder = new ThreadLocal<>();

    public VoiceHandler(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        this.voicechatApi = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    /**
     * Handles microphone packet events from SimpleVoiceChat.
     *
     * <p>Decodes Opus audio, calculates decibel level, and fires
     * VoiceDetectedEvent with the audio level. Each listener then
     * checks against their own threshold.
     *
     * @param event the microphone packet event
     */
    private void onMicrophonePacket(MicrophonePacketEvent event) {
        UUID playerId = event.getSenderConnection().getPlayer().getUuid();

        // Get audio data
        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData.length == 0) {
            return; // Empty packet (player stopped talking)
        }

        // Initialize a decoder per callback thread to avoid sharing decoder state.
        OpusDecoder opusDecoder = decoder.get();
        if (opusDecoder == null) {
            opusDecoder = event.getVoicechat().createDecoder();
            decoder.set(opusDecoder);
        }

        // Decode Opus to PCM samples
        opusDecoder.resetState();
        short[] samples = opusDecoder.decode(opusData);

        // Calculate audio level in decibels
        double db = AudioUtils.calculateAudioLevel(samples);

        // Resolve Bukkit state, run API hooks, and fire events on the main thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                return;
            }

            if (PluginPermissions.hasBypass(player)) {
                return;
            }

            Location voiceLoc = player.getLocation().add(0.0, VOICE_SOURCE_Y_OFFSET, 0.0);
            VoiceDetectionContext context = new VoiceDetectionContext(player, voiceLoc, db, db);
            plugin.getApi().callDetectionHooks(context);

            if (context.isCancelled()) {
                return;
            }

            // Debug logging
            if (plugin.getConfigManager().isAudioLoggingEnabled()) {
                plugin.getLogger().info(String.format(
                        "[Voice Debug] %s speaking at %.1f dB | %s",
                        player.getName(),
                        context.getEffectiveDecibels(),
                        AudioUtils.getDebugInfo(samples)
                ));
            }

            VoiceDetectedEvent voiceEvent = new VoiceDetectedEvent(player, voiceLoc, context.getEffectiveDecibels());
            Bukkit.getPluginManager().callEvent(voiceEvent);
        });
    }

    public VoicechatApi getVoicechatApi() {
        return voicechatApi;
    }
}
