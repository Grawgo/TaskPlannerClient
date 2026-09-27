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
3. В локальном `.env` Ktor-сервера указать идентификатор того же Firebase-проекта:

   ```properties
   FIREBASE_PROJECT_ID=идентификатор_проекта
   ```

   Сервер проверяет подпись ID-токена по открытым сертификатам Google и не
   использует закрытый ключ сервисного аккаунта.
4. Запустить PostgreSQL и Ktor-сервер в IntelliJ IDEA, затем приложение в
   Android Studio. При запуске на телефоне через USB выполнить
   `adb -d reverse tcp:8080 tcp:8080`; на эмуляторе этот шаг не нужен.
   Проверить регистрацию, повторный вход после перезапуска, создание задачи,
   выход и сброс пароля.

Пароль пользователя передаётся только Firebase по HTTPS. Ktor получает
лишь Firebase ID-токен и определяет владельца задачи по проверенному UID.
