# Настройка Firebase Authentication

Firebase в проекте отвечает только за регистрацию, вход, восстановление
пароля и выдачу ID-токена. Задачи хранятся в PostgreSQL через Ktor.
Firestore и Realtime Database не требуются.

1. В Firebase Console создать проект и включить способ входа
   **Authentication → Sign-in method → Email/Password**.
2. В настройках проекта Firebase найти **Web API key**. В корне Android-проекта
   создать `firebase.properties` по образцу `firebase.properties.example`:

   ```properties
   FIREBASE_API_KEY=ключ_из_Firebase_Console
   ```

   Файл исключён из Git. `google-services.json` этому клиенту не требуется.
3. Для Ktor-сервера в том же Firebase-проекте получить файл сервисного
   аккаунта в **Project settings → Service accounts**. Хранить JSON вне
   обоих репозиториев. В локальном `.env` сервера указать:

   ```properties
   FIREBASE_PROJECT_ID=идентификатор_проекта
   FIREBASE_CREDENTIALS_FILE=/абсолютный/путь/к/service-account.json
   ```

4. Запустить PostgreSQL и Ktor-сервер в IntelliJ IDEA, затем приложение в
   Android Studio. Проверить регистрацию, повторный вход после перезапуска,
   создание задачи, выход и сброс пароля.

Пароль пользователя передаётся только Firebase по HTTPS. Ktor получает
лишь Firebase ID-токен и определяет владельца задачи по проверенному UID.
