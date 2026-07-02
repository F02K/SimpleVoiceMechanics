# Simple Voice Mechanics

Realistic mob reactions to voice in Minecraft. Uses Simple Voice Chat for dynamic audio detection with volume and distance-based behavior.

## Features

- **Real-time Audio Analysis**: Opus decoding with dB calculation
- **Dynamic Range**: Louder voices heard from further away (40%-220% of base range)
- **Environmental Modifiers**: Biome & time-based acoustics (caves echo, forests dampen, night = more sensitive)
- **Vanilla+ Acoustics**: Rain, sneaking, wool, carpet, and solid blocks muffle voice
- **Mob Group Alerts**: Hostile mobs call reinforcements (zombies summon hordes, wolves hunt in packs)
- **Natural Behavior**: Probabilistic reactions, cooldowns, eye contact, investigation before attacks
- **4 Mob Categories**: Hostile (attack), Neutral (look), Peaceful (look/flee/follow), Warden (anger system)
- **Sculk Sensors**: Per-sensor voice activation on loud voices

### Mob Behavior

| Category | Reaction | Threshold | Special Features |
|----------|----------|-----------|------------------|
| **Hostile** | Investigate/Attack | -40 dB | Last heard location, then targeting and alerts |
| **Neutral** | Look (8s) | -35 dB | 60% reaction chance |
| **Peaceful** | Look (10s) | -30 dB | Flee at >-20 dB, Follow with temptation item |
| **Warden** | Anger +15-60 | -20 dB | Volume & distance dependent |
| **Sculk** | Activate | -20 dB | Only on loud voices |

**Peaceful Mobs Details:**
- Normal speaking: Look at player
- Loud speaking (>-20 dB): Flee 3 blocks away
- Sneaking + Speaking + Eye contact + temptation item: Follow for 60s

### Environmental Effects

| Environment | Range Modifier | Sensitivity | Example |
|-------------|---------------|-------------|---------|
| **Cave + Night** | ~2.0x | -13 dB | **VERY DANGEROUS** - Mobs hear whispers from far away |
| **Forest + Day** | 0.75x | +3 dB | Safer - Vegetation dampens sound |
| **Nether** | 1.3x | Normal | Sound carries in open spaces |
| **Mountain + Night** | 1.56x | -8 dB | Sound echoes across peaks |

**Mob Group Alerts:**
- Zombies: Call 5 allies within 16 blocks → Horde attack
- Wolves: Alert pack within 20 blocks → Coordinated hunt
- Piglins: Summon group within 18 blocks → Group combat

## Requirements

- Minecraft 1.18+ (Paper or compatible)
- Java 17+
- [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) Plugin

**Note:** Sculk Sensors require Minecraft 1.19+

## Installation

1. Install Simple Voice Chat
2. Place SimpleVoiceMechanics JAR in `plugins/` folder
3. Restart server
4. Adjust `plugins/SimpleVoiceMechanics/config.yml` as needed

## Configuration

### Global Settings
```yaml
detection:
  max-range: 16.0
  min-range: 2.0
  falloff-curve: 1.0        # 0.5-2.0 (0.5=linear, 2.0=steep)
```

### Environmental Modifiers
```yaml
environmental-modifiers:
  biome-modifiers:
    enabled: true           # Caves echo, forests dampen
  time-modifiers:
    enabled: true           # Night = 30% more range, 8 dB more sensitive
  weather-modifiers:
    enabled: true           # Rain/thunder muffle voice
```

### Vanilla+ Acoustic Modifiers
```yaml
detection:
  sneaking-reduction:
    enabled: true
    decibel-reduction: 6.0  # Sneaking makes voice quieter, not silent
  obstruction-muffling:
    enabled: true
    max-db-reduction: 18.0
    wool-db-reduction: 12.0
    solid-block-db-reduction: 4.0
```

### Sculk Sensor Range
```yaml
sculk-hearing:
  volume-threshold-db: -20.0
  range-offset: 0.0        # Added to each sensor's vanilla listener range
  min-range: 2.0
```

`range-offset` is signed: `0.0` keeps vanilla sensor range, positive values extend it, and negative values reduce it.

Voice-triggered sculk behavior uses a vanilla game event from the speaker position after plugin checks pass, so vanilla handles the sensor activation and vibration particle.

### Mob Group Alerts
```yaml
mob-hearing:
  hostile-mobs:
    group-alert:
      enabled: true
      only-after-targeting: true
      max-alerts: 5         # Max mobs to summon per detection
      ranges:
        zombie: 16.0        # Horde radius
        wolf: 20.0          # Pack radius
        piglin: 18.0        # Group radius
```

### Per-Category Settings
```yaml
peaceful-mobs:
  enabled: true
  volume-threshold-db: -30.0
  max-range: 8.0
  natural-behavior:
    reaction-chance: 0.7    # 70% reaction probability
    look-duration: 10       # Look duration in seconds
    reaction-cooldown: 3    # Cooldown between reactions
  flee-behavior:
    enabled: true
    flee-volume-db: -20.0
    flee-distance: 3.0
    flee-duration: 3
  follow-when-sneaking:
    enabled: true
    require-tempt-item: true
    require-eye-contact: true
    eye-contact-range: 4.0
    eye-contact-memory: 5
    duration: 60
    max-distance: 12.0
  blacklist: []             # Exclude EntityTypes
```

### Volume Scale (dB)

- **-60 dB**: Whispering
- **-40 dB**: Normal speaking (Default Hostile)
- **-30 dB**: Quiet to normal (Default Peaceful)
- **-20 dB**: Loud speaking (Default Warden/Sculk)
- **-10 dB**: Shouting

## Commands

- `/simplevoicemechanics reload` - Reload config
- `/simplevoicemechanics toggle <hostile|neutral|peaceful|warden|sculk>` - Toggle category
- `/simplevoicemechanics status` - Show current config

**Aliases:** `/svm`, `/voicemechanics`, `/voicelistener`, `/vl`

## Permissions

- `simplevoicemechanics.admin` (default: op)
- `simplevoicemechanics.bypass` (default: false)

Legacy aliases `voicelistener.admin` and `voicelistener.bypass` are still accepted for existing servers.

## Preset Examples

### Stealth Server
```yaml
hostile-mobs:
  volume-threshold-db: -50.0  # Whispers detected
  max-range: 24.0
  falloff-curve: 0.8
environmental-modifiers:
  biome-modifiers:
    enabled: true             # Caves are extra dangerous
  time-modifiers:
    enabled: true             # Nights are terrifying
```

### Action Server
```yaml
hostile-mobs:
  volume-threshold-db: -25.0  # Only loud speaking
  max-range: 12.0
  falloff-curve: 1.5
  group-alert:
    enabled: true
    max-alerts: 8             # Larger mob groups!
```

### Warden Extreme Challenge
```yaml
warden:
  volume-threshold-db: -60.0  # Hears everything!
  max-range: 32.0
  falloff-curve: 0.5
```

### Hardcore Survival
```yaml
environmental-modifiers:
  biome-modifiers:
    enabled: true
  time-modifiers:
    enabled: true
mob-hearing:
  hostile-mobs:
    volume-threshold-db: -50.0
    group-alert:
      enabled: true
      max-alerts: 10          # Massive hordes!
```

## Gameplay Examples

**Cave Exploration at Night:**
- Biome: Cave (1.5x range, -5 dB)
- Time: Night (1.3x range, -8 dB)
- **Combined: ~2x range, -13 dB more sensitive**
- Result: Whispering attracts zombies from far away. 1 zombie calls 5 more → 6 zombies attack!

**Forest Stealth (Day):**
- Biome: Forest (0.75x range, +3 dB)
- Time: Day (normal)
- **Combined: 0.75x range**
- Result: Vegetation dampens sound. Normal speaking is safe.

**Nether Fortress:**
- Biome: Nether (1.3x range)
- Mobs: Piglins (18 block alert)
- Result: 1 Piglin hears you → alerts 5 nearby Piglins → coordinated group attack

**Rainy Night:**
- Rain reduces voice range and makes detection harder
- Sneaking lowers voice impact further
- Wool or carpet between speaker and listener can prevent sculk-style detection

**Hostile Investigation:**
- First normal voice detection sends hostile mobs to the last heard position
- Repeated, close, or very loud voice escalates to direct targeting
- Group alerts happen after targeting by default

## Debug Options

```yaml
debug:
  audio-logging: false
  range-logging: false
  detection-logging: false
  warden-logging: false
  sculk-logging: false
  peaceful-logging: false
  environmental-logging: false
  group-alert-logging: false
```

## Technical Details

**Detection Flow:**
1. Audio packet received → Decode Opus → Calculate dB
2. Apply acoustic modifiers (biome, time, weather, sneaking)
3. Check category threshold → Calculate effective range
4. Probability-based detection → Natural behavior check
5. Apply cheap obstruction muffling for viable listeners
6. Execute action (Investigate/Attack/Look/Flee/Follow)
7. If hostile targeting escalates, alert nearby allies

**Formulas:**
```
Effective Range = Config Range × Biome Mult × Time Mult × (1.0 + (dB - Threshold) / 25.0)
Capped: 0.5× to 2.5×

Detection Probability = (1 - normalized_distance)^falloff_curve
100% at ≤ min-range, 0% at ≥ max-range

Mob Alerts = Dynamic (1-5 based on distance, closer = more alerts)
```

## API Usage

```java
@EventHandler
public void onVoiceDetected(VoiceDetectedEvent event) {
    Player player = event.getPlayer();
    double decibels = event.getDecibels();
    // Custom logic
}

// Access config
ConfigManager config = plugin.getConfigManager();
double threshold = config.getHostileVolumeThresholdDb();
boolean biomeMods = config.isBiomeModifiersEnabled();

// New utility classes
Biome biome = location.getBlock().getBiome();
double rangeMultiplier = BiomeModifier.getRangeMultiplier(biome);
boolean isNight = TimeModifier.isNight(world);
boolean isSocial = MobGroupAlert.isSocialMob(EntityType.ZOMBIE);
```

## Developer API

Other plugins can integrate through the Bukkit service API:

```java
import de.tecca.simplevoicemechanics.api.HookPriority;
import de.tecca.simplevoicemechanics.api.MobReactionType;
import de.tecca.simplevoicemechanics.api.SimpleVoiceMechanicsApi;
import org.bukkit.Location;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionVoiceBridge extends JavaPlugin {

    private SimpleVoiceMechanicsApi voiceApi;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<SimpleVoiceMechanicsApi> provider =
                getServer().getServicesManager().getRegistration(SimpleVoiceMechanicsApi.class);

        if (provider == null) {
            getLogger().warning("SimpleVoiceMechanics API is not available");
            return;
        }

        voiceApi = provider.getProvider();
        voiceApi.registerMobReactionHook(this, HookPriority.NORMAL, context -> {
            if (context.getReactionType() == MobReactionType.HOSTILE_TARGET
                    && isInProtectedRegion(context.getPlayer().getLocation())) {
                context.setCancelled(true);
            }
        });
    }

    @Override
    public void onDisable() {
        if (voiceApi != null) {
            voiceApi.unregisterHooks(this);
        }
    }

    private boolean isInProtectedRegion(Location location) {
        return false;
    }
}
```

Available service hooks:
- `registerDetectionHook` can cancel voice mechanics or adjust effective dB before normal processing.
- `registerMobReactionHook` can cancel specific mob reactions such as hostile targeting, invisible-player investigation, group alerts, fleeing, following, or Warden anger.
- `registerSculkActivationHook` can cancel individual sculk sensor activations.

Players with `simplevoicemechanics.bypass` do not trigger mob or sculk voice mechanics. The legacy `voicelistener.bypass` permission still works.

## Building

```bash
git clone https://github.com/yourusername/SimpleVoiceMechanics.git
cd SimpleVoiceMechanics
mvn clean package
```

## License

MIT License

## Credits

- Audio calculation adapted from Simple Voice Chat (henkelmax)
- Built for Paper
- SimpleVoiceChat API
- Environmental acoustics inspired by real-world sound propagation
