# Shopping List

An Android shopping-list application written in Kotlin. Users can register,
log in, and manage a private list of items. The Android application communicates
with a Ktor backend, and the backend stores accounts and items in MongoDB.

## Main features

- Account registration and login
- JWT authentication
- Add, edit, purchase, and delete shopping items
- A separate list for each account
- Password hashing with BCrypt
- MongoDB storage

## Project structure

```text
shopping-list/
├── .github/    GitHub repository configuration
├── android/    Android application
├── backend/    Ktor REST API
└── docker-compose.yml
```

## Requirements

- Android Studio
- JDK 17 or newer
- IntelliJ IDEA and MongoDB only if you want to run the backend locally

## Run from the IDE

### Run the Android app

The app is configured to use the hosted backend at:

```text
https://shopping-list-api-157d42b0.azurewebsites.net/
```

You do not need to start MongoDB or the backend in IntelliJ IDEA.

1. In IntelliJ IDEA or Android Studio, select **File > Open**.
2. Open the `android` folder as a project in a new window.
3. Wait for Gradle sync to finish.
4. Connect and unlock your phone.
5. Enable **Developer options > USB debugging** and accept the phone's prompt.
6. Select the `app` configuration and your phone in the top toolbar.
7. Click the green Run button.

### Run the backend locally (optional)

Start MongoDB first. Then:

1. Open the project root in IntelliJ IDEA.
2. Wait for Gradle sync to finish.
3. Open `backend/src/main/kotlin/com/example/shoppinglist/Application.kt`.
4. Click the green Run icon next to `fun main()`.

The backend starts at `http://localhost:8080`. Visit
`http://localhost:8080/health` to confirm it is running.

Optional backend environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | Backend port |
| `MONGODB_URI` | `mongodb://localhost:27017` | MongoDB connection |
| `MONGODB_DB` | `shopping_list` | Database name |
| `JWT_SECRET` | Local development value | Overrides the token-signing secret |

## API routes

| Method | Path | Description |
|---|---|---|
| GET | `/health` | Check the backend status |
| POST | `/auth/register` | Create an account |
| POST | `/auth/login` | Log in |
| GET | `/items` | Get the user's items |
| POST | `/items` | Add an item |
| PUT | `/items/{id}` | Update an item |
| DELETE | `/items/{id}` | Delete an item |

## Presentation order

1. Connect the phone and select it in the IDE.
2. Run the `app` configuration from Android Studio or IntelliJ IDEA.
3. Register, add an item, edit it, mark it purchased, and delete it.
