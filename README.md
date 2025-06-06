# Advanced AntiCheat Plugin for Minecraft

A comprehensive, version-compatible anticheat system for Minecraft Bukkit/Spigot servers supporting versions **1.8 through the latest releases**.

## 🚀 Version Compatibility

This plugin is designed to work seamlessly across multiple Minecraft versions:

- ✅ **Minecraft 1.8 - 1.21+**
- ✅ **Automatic version detection**
- ✅ **Feature adaptation based on server version**
- ✅ **Future-proof design for upcoming releases**

## 📋 Features

### Movement Checks
- **Fly Check**: Detects unauthorized flying and hovering
  - Elytra support (1.9+)
  - Creative mode exemption
- **Speed Check**: Monitors movement speed with potion effect support
- **NoFall Check**: Prevents fall damage bypassing

### Combat Checks
- **KillAura Check**: Detects automated combat behavior
  - Angle-based detection
  - Attack frequency analysis
- **Reach Check**: Monitors attack distance with configurable limits

### Block Checks
- **FastBreak Check**: Prevents rapid block breaking
  - Tool efficiency calculation
  - Potion effect consideration

### Inventory Checks
- **Inventory Check**: Detects rapid inventory manipulation

## 🔧 Building the Plugin

### Prerequisites
- Java 8 or higher
- Git (optional)

### Quick Build

**Linux/Mac:**
```bash
chmod +x build.sh
./build.sh
```

**Windows:**
```batch
build.bat
```

**Manual Build:**
```bash
./gradlew clean build
```

## 📦 Installation

1. Build the plugin using instructions above
2. Copy `build/libs/AdvancedAntiCheat-1.0.jar` to your server's `plugins/` directory
3. Start/restart your server
4. Configure via `plugins/AdvancedAntiCheat/config.yml`

## ⚙️ Configuration

### Version-Specific Settings

```yaml
anticheat:
  compatibility:
    auto-adjust: true          # Automatically adjust for server version
    disable-incompatible: true # Disable unsupported checks
    
  checks:
    fly:
      check-elytra: true      # Only works on 1.9+ (auto-disabled on older versions)
```

### Universal Settings

All checks work across versions with automatic adaptation:

```yaml
anticheat:
  checks:
    speed:
      threshold: 1.3          # Speed multiplier threshold
    reach:
      max-distance: 4.2       # Maximum reach distance
    fastbreak:
      tolerance: 0.8          # Break time tolerance
```

## 🎯 Version-Specific Features

| Feature | 1.8 | 1.9+ | 1.13+ | 1.16+ | Notes |
|---------|-----|------|-------|-------|-------|
| Basic Checks | ✅ | ✅ | ✅ | ✅ | All versions |
| Elytra Detection | ❌ | ✅ | ✅ | ✅ | Auto-disabled on 1.8 |
| Modern Materials | ❌ | ❌ | ✅ | ✅ | Fallback for older versions |
| Off-hand Support | ❌ | ✅ | ✅ | ✅ | Automatic detection |
| Hex Colors | ❌ | ❌ | ❌ | ✅ | Message formatting |

## 🎮 Commands

### Main Command
```
/anticheat [reload|info|violations|clear|alerts]
```

**Aliases:** `/ac`

### Subcommands
- `/ac reload` - Reload configuration
- `/ac info` - Show plugin information
- `/ac violations <player>` - View player violations
- `/ac clear <player>` - Clear player violations
- `/ac alerts` - Toggle violation alerts

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `anticheat.admin` | Access to all commands | OP |
| `anticheat.bypass` | Bypass all checks | false |
| `anticheat.alerts` | Receive violation alerts | OP |

## 📊 Violation System

### Violation Levels
Each check has configurable violation thresholds:
- **Level 1**: Warning/Kick
- **Level 2**: Temporary ban
- **Level 3**: Permanent ban (if enabled)

### Punishment Commands
Configure custom punishment commands in `config.yml`:

```yaml
punishments:
  fly:
    - "kick %player% Suspicious flying detected"
    - "tempban %player% 1h Flying is not allowed"
    - "ban %player% Repeated flying violations"
```

## 🔍 Alert System

### Alert Format
```
[AC] PlayerName failed FlyCheck (VL: 3)
```

### Alert Recipients
- Console (configurable)
- Staff members with `anticheat.alerts` permission
- Custom webhook support (planned)

## 🛠️ Technical Details

### Version Detection
The plugin automatically detects your server version and adapts:

```java
// Example version detection
if (VersionUtils.isVersionOrHigher(13)) {
    // Use modern Material names
    Material.GRASS_BLOCK
} else {
    // Use legacy Material names
    Material.GRASS
}
```

### Material Compatibility
Automatic fallback for material names across versions:
- `GRASS_BLOCK` (1.13+) → `GRASS` (1.8-1.12)
- `OAK_LOG` (1.13+) → `LOG` (1.8-1.12)
- And many more...

### API Compatibility
Safe API usage with version checks:
- Off-hand support detection
- Elytra compatibility
- Modern enchantment handling

## 🐛 Troubleshooting

### Common Issues

**Build Errors:**
```bash
# Clean and rebuild
./gradlew clean build
```

**Version Compatibility:**
- Check console for version detection logs
- Verify `api-version` in plugin.yml
- Ensure Java version compatibility

**False Positives:**
- Adjust sensitivity in config.yml
- Check for conflicting plugins
- Review violation thresholds

### Debug Mode
Enable debug mode for detailed logging:

```yaml
anticheat:
  debug: true
```

## 📈 Performance

### Optimizations
- Efficient data structures
- Minimal memory footprint
- Asynchronous processing where possible
- Automatic cleanup of old data

### Resource Usage
- **RAM**: ~5-10MB typical usage
- **CPU**: <1% on average server
- **Network**: Minimal impact

## 🔄 Updates & Compatibility

### Automatic Updates
The plugin is designed to work with future Minecraft versions without modification:

1. **Version Detection**: Automatically detects new versions
2. **Feature Adaptation**: Disables incompatible features gracefully
3. **Material Handling**: Uses string-based material detection
4. **API Safety**: Wrapped API calls with version checks

### Manual Updates
For major Minecraft updates:
1. Check for plugin updates
2. Test on development server
3. Update configuration if needed

## 🤝 Contributing

### Development Setup
```bash
git clone <repository-url>
cd minecraft-anticheat-plugin
./gradlew build
```

### Code Style
- Follow Java conventions
- Add version compatibility comments
- Test across multiple versions
- Document new features

### Submitting Changes
1. Fork the repository
2. Create feature branch
3. Test on multiple versions
4. Submit pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

### Getting Help
- Check this README first
- Review configuration examples
- Enable debug mode for detailed logs
- Check console for error messages

### Reporting Issues
When reporting issues, include:
- Minecraft version
- Server software (Spigot/Paper/etc.)
- Plugin version
- Error logs
- Configuration file

### Contact
- GitHub Issues: [Create an issue](https://github.com/yourusername/minecraft-anticheat-plugin/issues)
- Discord: YourDiscord#1234
- Email: your.email@example.com

## 🎉 Acknowledgments

- Bukkit/Spigot development team
- Minecraft server community
- Contributors and testers
- Plugin developers who inspired this project

---

**Made with ❤️ for the Minecraft community**

*Compatible with Minecraft 1.8 through latest versions*