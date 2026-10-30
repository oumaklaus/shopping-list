# Shopping List

A beginner-friendly Android shopping list app built with Kotlin and MongoDB.

Users can add items with a name and quantity, see the whole list, mark items as
purchased or not purchased, edit item details, and delete items. Data is stored
in MongoDB, so it survives app and server restarts.

## Quick start (one command)

Connect your Android phone with USB debugging enabled, unlock it, and accept the
"Allow USB debugging" prompt. Then run:

```bash
./run.sh
```

This single command:

1. starts MongoDB if it is not already running;
2. builds and starts the Ktor backend on port 8080;
3. forwards port 8080 to the phone with `adb reverse`;
4. builds the Android app with the correct URL for the phone;
5. installs and opens the app.

Keep the phone connected by USB while you test. The backend log is at
`.run/backend.log`.

Useful variations:

```bash
./run.sh          # start everything and open the app
./run.sh backend  # start only MongoDB and the backend
./run.sh app      # build/install/open the app (backend must already run)
./run.sh cloud https://YOUR-APP.azurewebsites.net
./run.sh stop     # stop the backend; MongoDB keeps running
```

The script also works with an emulator. If both a phone and an emulator are
connected, it prefers the physical phone. Set `ANDROID_SERIAL` to choose a
specific device.

## Architecture

```
Android app (Kotlin, XML + RecyclerView)
        |
        |  HTTP / JSON (Retrofit)
        v
Ktor REST API (Kotlin)
        |
        |  MongoDB driver
        v
MongoDB Atlas (Docker is an optional local fallback)
```

An Android app cannot connect to MongoDB directly, so the project uses a small
Ktor backend in Kotlin. That keeps the whole project in Kotlin while still using
MongoDB properly.

## Project structure

```
shopping-list/
├── android/                 Android app (open this folder in Android Studio)
│   └── app/src/main/
│       ├── java/com/example/shoppinglist/
│       │   ├── data/        Item model, Retrofit API, repository
│       │   ├── ui/          ViewModel, adapter, input validation
│       │   └── MainActivity.kt
│       └── res/             Layouts, theme, strings, icons
├── backend/                 Ktor REST API
│   └── src/main/kotlin/com/example/shoppinglist/
│       ├── model/           Shared data classes
│       ├── repository/      Repository interface + MongoDB implementation
│       └── routes/          HTTP routes
├── docker-compose.yml       MongoDB for local development
├── deploy-azure.sh          Azure App Service deployment helper
├── run.sh                   One-command local setup and phone install
└── README.md
```

## Requirements

- Android Studio (with an emulator or a physical Android device)
- JDK 17 or newer
- Docker for optional local MongoDB, or a MongoDB Atlas connection string
- Azure account and Cosmos DB for MongoDB (or Atlas) when deploying

## 1. Start MongoDB

With Docker Compose:

```bash
docker compose up -d --wait
```

If Docker Compose is not available, use plain Docker:

```bash
docker run -d --name shopping-list-mongo \
  -p 127.0.0.1:27017:27017 \
  -v shopping_list_mongo_data:/data/db \
  mongo:7.0
```

If Docker reports `failed to add the host <=> sandbox pair interfaces` (some
sandboxes do not support bridge networking), use host networking instead:

```bash
docker run -d --name shopping-list-mongo \
  --network host \
  -v shopping_list_mongo_data:/data/db \
  mongo:7.0 --bind_ip 127.0.0.1
```

The backend uses `mongodb://localhost:27017` and the database
`shopping_list` by default.

To use MongoDB Atlas instead, set `MONGODB_URI` before starting the backend:

```bash
read -rsp "Atlas URI: " MONGODB_URI && export MONGODB_URI && printf '\n'
```

## 2. Run the backend

The easiest way is the one-command script from the project root:

```bash
./run.sh backend
```

Or run the backend directly with Gradle:

```bash
cd backend
./gradlew run
```

If `./gradlew run` fails with `Address already in use`, an older backend is
still running on port 8080. Use `./run.sh stop` or stop that process first.

The API starts at `http://localhost:8080`. Check it with:

```bash
curl http://localhost:8080/health
```

Expected response:

```json
{"status":"ok"}
```

Environment variables:

| Variable      | Default                     | Purpose                  |
|---------------|-----------------------------|--------------------------|
| `PORT`        | `8080`                      | HTTP port                |
| `MONGODB_URI` | `mongodb://localhost:27017` | MongoDB connection string |
| `MONGODB_DB`  | `shopping_list`             | Database name            |

## Cloud deployment (Azure)

The Shopping List uses its own Azure App Service, Mongo-compatible database,
credentials, and URL. Do not reuse TaskFlow resources.

1. Install Azure CLI and run `az login`.
2. Create a separate Cosmos DB for MongoDB database (or Atlas database) and get
   its connection string.
3. Deploy with Shopping List-specific values:

```bash
export AZURE_RESOURCE_GROUP="shopping-list-school-rg"
export AZURE_APP_NAME="UNIQUE-SHOPPING-LIST-API"
export AZURE_LOCATION="eastus"
export AZURE_SKU="F1"
read -rsp "MongoDB URI: " MONGODB_URI && export MONGODB_URI && printf '\n'
export MONGODB_DB="shopping_list"
./deploy-azure.sh
```

`F1` is requested by default to minimize cost, but availability and charges
depend on the subscription and region.

After deployment, install the cloud-configured APK:

```bash
./run.sh cloud https://UNIQUE-SHOPPING-LIST-API.azurewebsites.net
```

The installed app then talks directly to Azure, so Docker and the local backend
are no longer required.

## 3. Run the Android app

### Physical phone (recommended)

Connect the phone by USB with USB debugging enabled, then run this from the
project root:

```bash
./run.sh
```

The script runs `adb reverse tcp:8080 tcp:8080` and builds the app with
`http://127.0.0.1:8080/`. That means the phone does not need to be on the same
Wi-Fi network as the computer, and no LAN IP or firewall changes are needed.
Keep the USB cable connected while testing.

### Emulator or Android Studio

1. Open the `android/` folder in Android Studio.
2. Wait for Gradle sync to finish.
3. Start an emulator and run the `app` configuration.

The emulator reaches the computer through `10.0.2.2`, which is the default URL:

```kotlin
val apiBaseUrl = providers.gradleProperty("apiBaseUrl")
    .getOrElse("http://10.0.2.2:8080/")
```

You can override it at build time for any other setup:

```bash
cd android
./gradlew assembleDebug -PapiBaseUrl="http://192.168.1.20:8080/"
```

The backend must be running before you launch the app.

## API

| Method | Path          | Description                    | Success |
|--------|---------------|--------------------------------|---------|
| GET    | `/health`     | Health check                   | 200     |
| GET    | `/items`      | List all items                 | 200     |
| POST   | `/items`      | Create an item                 | 201     |
| PUT    | `/items/{id}` | Update name, quantity, status  | 200     |
| DELETE | `/items/{id}` | Delete an item                 | 204     |

Create an item:

```bash
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{"name":"Milk","quantity":2}'
```

Update an item:

```bash
curl -X PUT http://localhost:8080/items/ITEM_ID \
  -H "Content-Type: application/json" \
  -d '{"name":"Oat milk","quantity":3,"purchased":true}'
```

Delete an item:

```bash
curl -X DELETE http://localhost:8080/items/ITEM_ID
```

Validation rules: the name must not be blank and must be at most 80
characters; the quantity must be an integer from 1 to 9999.

An item looks like this:

```json
{
  "id": "6ae10dfd191ea823cfc29492",
  "name": "Milk",
  "quantity": 2,
  "purchased": false,
  "createdAt": 1793134077565
}
```

## Tests

Backend unit tests (10 tests; Ktor routes with an in-memory repository):

```bash
cd backend
./gradlew test
```

Azure executable-JAR build from the repository root:

```bash
./gradlew clean azurePackage
```

Android unit tests (21 tests: 7 input validation, 8 ViewModel, 6 Retrofit/API
contract tests using MockWebServer):

```bash
cd android
./gradlew testDebugUnitTest
```

Android build and lint:

```bash
cd android
./gradlew assembleDebug
./gradlew lintDebug
```

The project was also verified end-to-end on an Android 15 emulator against the
real Ktor backend and MongoDB:

- add an item, then see it in the list;
- toggle purchased and confirm the change in MongoDB;
- edit the name/quantity and confirm the update;
- delete the item and see the empty state;
- restart the app and confirm the list reloads from MongoDB;
- stop the backend, confirm the error screen appears, restart the backend, and
  confirm Retry reloads the list.

The one-command setup was also verified on a physical Android phone: the script
installed and launched the app, the app loaded the list through `adb reverse`,
and a new item added on the phone was saved to MongoDB.

## Notes and limitations

- There is one shared shopping list and no login. This keeps the project focused
  on Android and MongoDB CRUD for a school demo.
- HTTP is used for local development. `android:usesCleartextTraffic="true"` is
  only appropriate for a local demo; use HTTPS in a real app.
- The app shows a clear error screen when the backend cannot be reached, with a
  Retry button.
- Purchased items are shown with strikethrough text and are sorted by creation
  time along with the rest of the list.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| App shows "Could not reach the server" | Make sure MongoDB and the backend are running, and the base URL is correct. |
| Emulator cannot connect | Use `http://10.0.2.2:8080/`, not `localhost`. |
| Physical phone cannot connect | Run `./run.sh` from the project root; it sets up `adb reverse` automatically. Keep the USB cable connected. |
| `No Android device detected` | Enable USB debugging, unlock the phone, accept the "Allow USB debugging" prompt, then run `./run.sh` again. |
| Device shows as `unauthorized` | Unlock the phone and accept the USB debugging prompt, then reconnect the cable. |
| Port 8080 is already in use | Run `./run.sh` (it stops the previous project backend) or `./run.sh stop`. |
| Backend starts but requests fail | Check `MONGODB_URI` and make sure MongoDB is accepting connections on port 27017. |
| Gradle cannot find the Android SDK | Open the `android/` folder in Android Studio, or set `sdk.dir` in `android/local.properties`. |
