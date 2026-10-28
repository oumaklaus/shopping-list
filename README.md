# Shopping List

A beginner-friendly Android shopping list app built with Kotlin and MongoDB.

Users can add items with a name and quantity, see the whole list, mark items as
purchased or not purchased, edit item details, and delete items. Data is stored
in MongoDB, so it survives app and server restarts.

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
MongoDB (Docker or MongoDB Atlas)
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
└── README.md
```

## Requirements

- Android Studio (with an emulator or a physical Android device)
- JDK 17 or newer
- Docker (recommended) or a MongoDB connection string

## 1. Start MongoDB

With Docker Compose:

```bash
docker compose up -d --wait
```

If Docker Compose is not available, use plain Docker:

```bash
docker run -d --name shopping-list-mongo \
  -p 27017:27017 \
  -v shopping_list_mongo_data:/data/db \
  mongo:7.0
```

If Docker reports `failed to add the host <=> sandbox pair interfaces` (some
sandboxes do not support bridge networking), use host networking instead:

```bash
docker run -d --name shopping-list-mongo \
  --network host \
  -v shopping_list_mongo_data:/data/db \
  mongo:7.0
```

The backend uses `mongodb://localhost:27017` and the database
`shopping_list` by default.

To use MongoDB Atlas instead, set `MONGODB_URI` before starting the backend:

```bash
export MONGODB_URI="mongodb+srv://USER:PASSWORD@CLUSTER.mongodb.net/?retryWrites=true&w=majority"
```

## 2. Run the backend

```bash
cd backend
./gradlew run
```

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

## 3. Run the Android app

1. Open the `android/` folder in Android Studio.
2. Wait for Gradle sync to finish.
3. Start an emulator and run the `app` configuration.

The emulator reaches the computer through `10.0.2.2`, so the app is configured
with:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
```

For a physical phone, change that line in
`android/app/build.gradle.kts` to your computer's LAN IP, for example:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.20:8080/\"")
```

The phone and computer must be on the same network, and the backend must be
running. Rebuild the app after changing the URL.

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
| Physical phone cannot connect | Use the computer's LAN IP, keep both devices on the same Wi-Fi, and allow port 8080 through the firewall. |
| Backend starts but requests fail | Check `MONGODB_URI` and make sure MongoDB is accepting connections on port 27017. |
| Port 8080 is already in use | Start the backend with another port: `PORT=8081 ./gradlew run`, then update `API_BASE_URL`. |
| Gradle cannot find the Android SDK | Open the `android/` folder in Android Studio, or set `sdk.dir` in `android/local.properties`. |
