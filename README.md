# Pokrio - Poker Backend Server

Free poker site to play with your friends: available at https://callingstation.vaporized.space/

## Overview

Pokrio is a Kotlin-based poker backend server that provides real-time multiplayer poker games. It uses Ktor for HTTP web serving and Socket.IO for real-time communication between players.

## Prerequisites

- Java 23 or higher
- Gradle (included via wrapper)

## Tech Stack

- **Language**: Kotlin 2.2.20
- **Web Framework**: Ktor 3.0.1
- **Real-time Communication**: Socket.IO (netty-socketio)
- **Database**: SQLite with Exposed ORM
- **Build Tool**: Gradle with Kotlin DSL
- **Logging**: SLF4J with Logback

## Project Structure

```
├── src/main/java/io/pokr/          # Main source code
│   ├── config/                     # Configuration classes
│   ├── network/                    # Web and Socket engines
│   ├── game/                       # Game logic and models
│   ├── database/                   # Database layer
│   └── serialization/              # Game state serialization
├── src/test/                       # Unit tests
├── db/                            # SQLite database files
├── logs/                          # Application logs
├── state/                         # Game state persistence
└── tools/                         # Deployment and testing tools
```

## Environment Setup

### 1. Create Environment File

Copy the environment template and configure it:

```powershell
# Create .env file with required configuration
New-Item -Path ".env" -ItemType File -Force
```

Add the following environment variables to your `.env` file:

```env
# Environment (debug/production)
ENV=debug

# Web server configuration
WEB_URL=http://localhost
WEB_PORT=8080

# Socket.IO server configuration  
SOCKETS_PORT=8081
SOCKET_ADDRESS=http://localhost:8081
```

### 2. Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `ENV` | Environment mode (optional, defaults to debug) | `debug` or `production` |
| `WEB_URL` | Base URL for the web server | `http://localhost` |
| `WEB_PORT` | Port for HTTP web server | `8080` |
| `SOCKETS_PORT` | Port for Socket.IO server | `8081` |
| `SOCKET_ADDRESS` | Socket.IO server address (optional, defaults to WEB_URL) | `http://localhost:8081` |

## Building and Running

### Development Mode

```powershell
# Run the application directly
.\gradlew.bat run

# Or run with Gradle wrapper
gradle run
```

### Build JAR

```powershell
# Build regular JAR
.\gradlew.bat build

# The JAR will be created in build/libs/
```

### Build Fat JAR (Recommended for Deployment)

```powershell
# Build fat JAR with all dependencies included
.\gradlew.bat fatJar

# The fat JAR will be created as build/libs/pokrio-1.1.jar
```

### Run Fat JAR

```powershell
# Run the fat JAR
java -jar build/libs/pokrio-1.1.jar
```

## Database

The application uses SQLite database stored in the `db/` directory. The database file (`pokrio.db`) will be created automatically on first run.

## Logging

Application logs are stored in the `logs/` directory:
- Current log: `logs/pokrio.log`
- Archived logs: `logs/pokrio.log.YYYY-MM-DD`

## Game State Persistence

Game states are persisted in JSON format in the `state/` directory to allow recovery after server restarts.

## Development Tools

### Load Testing

A Node.js load testing tool is available in `tools/load-tester/`:

```powershell
cd tools/load-tester
npm install
node index.js
```

### SSL Certificate Renewal

For production deployments, use the certificate renewal script:

```bash
./tools/renew-cert.sh
```

## API Endpoints

The server provides both HTTP and WebSocket endpoints:

- **Web Interface**: `http://localhost:8080`
- **Socket.IO**: `http://localhost:8081`

## Testing

```powershell
# Run all tests
.\gradlew.bat test

# Run tests with detailed output
.\gradlew.bat test --info
```

## Production Deployment

1. Set environment variables for production:
   ```env
   ENV=production
   WEB_URL=https://your-domain.com
   WEB_PORT=80
   SOCKETS_PORT=443
   ```

2. Build fat JAR:
   ```powershell
   .\gradlew.bat fatJar
   ```

3. Deploy and run:
   ```powershell
   java -jar build/libs/pokrio-1.1.jar
   ```

## Troubleshooting

### Common Issues

1. **".env file not found" error**: Ensure you have created a `.env` file with required variables
2. **Port already in use**: Change the port numbers in your `.env` file
3. **Java version issues**: Ensure you're using Java 23 or higher

### Logs

Check the application logs in `logs/pokrio.log` for detailed error information.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `.\gradlew.bat test`
5. Build the project: `.\gradlew.bat build`
6. Submit a pull request

## License

This project is open source. Please check the license file for details.
