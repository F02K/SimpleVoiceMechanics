package de.tecca.simplevoicemechanics.api;

import de.tecca.simplevoicemechanics.SimpleVoiceMechanics;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Default Bukkit service implementation for SimpleVoiceMechanics.
 */
public final class SimpleVoiceMechanicsApiImpl implements SimpleVoiceMechanicsApi {

    private final SimpleVoiceMechanics plugin;
    private final List<DetectionRegistration> detectionHooks = new ArrayList<>();
    private final List<MobReactionRegistration> mobReactionHooks = new ArrayList<>();
    private final List<SculkActivationRegistration> sculkActivationHooks = new ArrayList<>();

    public SimpleVoiceMechanicsApiImpl(SimpleVoiceMechanics plugin) {
        this.plugin = plugin;
    }

    @Override
    public VoiceHookRegistration registerDetectionHook(Plugin owner, HookPriority priority, VoiceDetectionHook hook) {
        DetectionRegistration registration = new DetectionRegistration(owner, priority, hook);
        detectionHooks.add(registration);
        sort(detectionHooks);
        return registration;
    }

    @Override
    public VoiceHookRegistration registerMobReactionHook(Plugin owner, HookPriority priority, VoiceMobReactionHook hook) {
        MobReactionRegistration registration = new MobReactionRegistration(owner, priority, hook);
        mobReactionHooks.add(registration);
        sort(mobReactionHooks);
        return registration;
    }

    @Override
    public VoiceHookRegistration registerSculkActivationHook(Plugin owner, HookPriority priority,
                                                            VoiceSculkActivationHook hook) {
        SculkActivationRegistration registration = new SculkActivationRegistration(owner, priority, hook);
        sculkActivationHooks.add(registration);
        sort(sculkActivationHooks);
        return registration;
    }

    @Override
    public void unregisterHooks(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        unregisterOwned(detectionHooks, owner);
        unregisterOwned(mobReactionHooks, owner);
        unregisterOwned(sculkActivationHooks, owner);
    }

    @Override
    public int getDetectionHookCount() {
        return detectionHooks.size();
    }

    @Override
    public int getMobReactionHookCount() {
        return mobReactionHooks.size();
    }

    @Override
    public int getSculkActivationHookCount() {
        return sculkActivationHooks.size();
    }

    public void callDetectionHooks(VoiceDetectionContext context) {
        for (DetectionRegistration registration : snapshot(detectionHooks)) {
            if (!registration.isRegistered()) {
                continue;
            }
            try {
                registration.hook.handleVoiceDetection(context);
            } catch (Throwable throwable) {
                logHookFailure(registration, throwable);
            }
        }
    }

    public void callMobReactionHooks(VoiceMobReactionContext context) {
        for (MobReactionRegistration registration : snapshot(mobReactionHooks)) {
            if (!registration.isRegistered()) {
                continue;
            }
            try {
                registration.hook.handleMobReaction(context);
            } catch (Throwable throwable) {
                logHookFailure(registration, throwable);
            }
        }
    }

    public void callSculkActivationHooks(VoiceSculkActivationContext context) {
        for (SculkActivationRegistration registration : snapshot(sculkActivationHooks)) {
            if (!registration.isRegistered()) {
                continue;
            }
            try {
                registration.hook.handleSculkActivation(context);
            } catch (Throwable throwable) {
                logHookFailure(registration, throwable);
            }
        }
    }

    private <T extends AbstractRegistration> void sort(List<T> registrations) {
        registrations.sort(Comparator.comparingInt(registration -> registration.getPriority().ordinal()));
    }

    private <T extends AbstractRegistration> List<T> snapshot(List<T> registrations) {
        return new ArrayList<>(registrations);
    }

    private <T extends AbstractRegistration> void unregisterOwned(List<T> registrations, Plugin owner) {
        for (T registration : snapshot(registrations)) {
            if (registration.isOwnedBy(owner)) {
                registration.unregister();
            }
        }
    }

    private void logHookFailure(AbstractRegistration registration, Throwable throwable) {
        plugin.getLogger().log(
                Level.WARNING,
                "Plugin " + registration.owner.getName() + " failed in a SimpleVoiceMechanics hook",
                throwable
        );
    }

    private abstract class AbstractRegistration implements VoiceHookRegistration {

        private final Plugin owner;
        private final HookPriority priority;
        private boolean registered = true;

        private AbstractRegistration(Plugin owner, HookPriority priority) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.priority = Objects.requireNonNull(priority, "priority");
        }

        @Override
        public Plugin getOwner() {
            return owner;
        }

        @Override
        public HookPriority getPriority() {
            return priority;
        }

        @Override
        public boolean isRegistered() {
            return registered;
        }

        @Override
        public void unregister() {
            if (!registered) {
                return;
            }
            registered = false;
            remove();
        }

        boolean isOwnedBy(Plugin plugin) {
            return owner.equals(plugin);
        }

        protected abstract void remove();
    }

    private final class DetectionRegistration extends AbstractRegistration {
        private final VoiceDetectionHook hook;

        private DetectionRegistration(Plugin owner, HookPriority priority, VoiceDetectionHook hook) {
            super(owner, priority);
            this.hook = Objects.requireNonNull(hook, "hook");
        }

        @Override
        protected void remove() {
            detectionHooks.remove(this);
        }
    }

    private final class MobReactionRegistration extends AbstractRegistration {
        private final VoiceMobReactionHook hook;

        private MobReactionRegistration(Plugin owner, HookPriority priority, VoiceMobReactionHook hook) {
            super(owner, priority);
            this.hook = Objects.requireNonNull(hook, "hook");
        }

        @Override
        protected void remove() {
            mobReactionHooks.remove(this);
        }
    }

    private final class SculkActivationRegistration extends AbstractRegistration {
        private final VoiceSculkActivationHook hook;

        private SculkActivationRegistration(Plugin owner, HookPriority priority, VoiceSculkActivationHook hook) {
            super(owner, priority);
            this.hook = Objects.requireNonNull(hook, "hook");
        }

        @Override
        protected void remove() {
            sculkActivationHooks.remove(this);
        }
    }
}
