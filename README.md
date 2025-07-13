# SentinelAC

Advanced Anti-Cheat plugin for Minecraft servers, inspired by GrimAC architecture with Vulcan-style configuration.

## Features

- **Packet-Level Analysis**: Uses ProtocolLib for deep packet inspection
- **Comprehensive Check System**: Movement, combat, and interaction checks
- **Violation Management**: Smart violation tracking with automatic actions
- **Alert System**: Console, in-game, and webhook notifications
- **Lag Compensation**: Intelligent handling of server and client lag
- **Exemption System**: Permission-based and temporary exemptions

## Architecture

### Core Components

1. **SentinelAC (Main Class)**: Plugin entry point with lifecycle management
2. **ConfigManager**: Thread-safe configuration handling with caching
3. **PlayerDataManager**: Per-player data tracking with automatic cleanup
4. **CheckManager**: Check registration and execution coordination
5. **ViolationManager**: Violation processing and action handling
6. **AlertManager**: Multi-channel alert system with webhook support
7. **PacketListener**: ProtocolLib integration for packet analysis

### Package Structure

```
com.sentinelac/
├── SentinelAC.java              # Main plugin class
├── managers/                    # Core management classes
│   ├── ConfigManager.java       # Configuration management
│   ├── PlayerDataManager.java   # Player data tracking
│   ├── CheckManager.java        # Check coordination
│   ├── ViolationManager.java    # Violation handling
│   └── AlertManager.java        # Alert system
├── data/
│   └── PlayerData.java          # Per-player data container
├── checks/
│   └── Check.java               # Base check class
├── listeners/
│   └── PacketListener.java      # Packet processing
└── commands/
    └── SentinelCommand.java     # Command handler
```

## Dependencies

- **Spigot/Paper API**: 1.21+
- **ProtocolLib**: 5.1.0+ (Required)
- **Java**: 21+ (Required for Paper 1.21)

## Installation

1. Ensure you have ProtocolLib installed
2. Download SentinelAC jar file
3. Place in your server's `plugins/` directory
4. Restart your server
5. Configure settings in `plugins/SentinelAC/config.yml`

## Configuration

### Main Configuration (`config.yml`)
- General settings (debug, async processing, ping limits)
- Threading configuration
- Violation management settings
- Alert system configuration
- Performance tuning options

### Check Configuration (`checks.yml`)
- Individual check enable/disable
- Per-check violation thresholds
- Check-specific parameters

### Alert Configuration (`alerts.yml`)
- Customizable alert messages
- Per-check alert formats
- Webhook integration settings

## Commands

- `/sentinel help` - Show help menu
- `/sentinel info [player]` - Show player information
- `/sentinel reload` - Reload configuration
- `/sentinel alerts <toggle|test>` - Manage alerts
- `/sentinel violations <player> [check|reset]` - Manage violations
- `/sentinel stats` - Show plugin statistics
- `/sentinel debug <on|off|player>` - Debug utilities
- `/sentinel webhook <test|url>` - Webhook management

## Permissions

- `sentinelac.*` - All permissions
- `sentinelac.alerts` - Receive alerts
- `sentinelac.exempt` - Exempt from all checks
- `sentinelac.command.*` - All commands

## Building

```bash
./gradlew clean build
```

The built jar will be available in `build/libs/SentinelAC-1.0.0.jar`

## Development

### Threading Model
- **Main Thread**: Bukkit/Spigot event handling
- **Worker Pool**: Asynchronous check processing
- **Packet Threads**: ProtocolLib packet processing
- **Cleanup Thread**: Background data maintenance

### Performance Considerations
- Cached configuration values for frequent access
- Concurrent data structures for thread safety
- Lag compensation and exemption systems
- Efficient packet filtering and processing

### Extensibility
- Modular check system for easy additions
- Event-driven architecture
- Plugin API for external integrations
- Configurable action system

## Planned Features

- [ ] Database integration for violation persistence
- [ ] Web dashboard for monitoring
- [ ] Machine learning detection enhancements
- [ ] Additional check implementations
- [ ] Advanced statistical analysis
- [ ] Cloud-based signature updates

## Support

For support, feature requests, or bug reports, please visit our GitHub repository or Discord server.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
