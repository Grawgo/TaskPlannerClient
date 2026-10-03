# Конфигурация Firebase Authentication

Firebase отвечает за регистрацию, вход, восстановление пароля и выдачу
ID-токена. Задачи хранятся в PostgreSQL через Ktor. Firestore и Realtime
Database не используются.

## Firebase-проект

Для аутентификации используется способ входа **Email/Password**.
Он доступен в Firebase Console в разделе
**Authentication → Sign-in method**. Web API key и идентификатор проекта
находятся в настройках Firebase-проекта.

## Android-клиент

Web API key задаётся в `firebase.properties` в корне Android-проекта:

```properties
FIREBASE_API_KEY=ключ_из_Firebase_Console
```

Образец конфигурации: `firebase.properties.example`. Локальный файл
исключён из Git. Клиент использует REST API Firebase; файл
`google-services.json` не требуется.

## Ktor-сервер

В `.env` сервера задаётся идентификатор того же Firebase-проекта:

```properties
FIREBASE_PROJECT_ID=идентификатор_проекта
```

Сервер проверяет подпись ID-токена по открытым сертификатам Google.
Закрытый ключ сервисного аккаунта не используется.

## Сетевое подключение

Ktor запускается с `SERVER_HOST=0.0.0.0` и подключается к локальной
PostgreSQL. Телефон в той же Wi-Fi сети автоматически обнаруживает сервер
по UDP-порту `45678`. USB не требуется. Эмулятор обращается к Ktor
через `http://10.0.2.2:8080/`.

Firebase Authentication на клиенте и загрузка сертификатов Google
на сервере требуют доступа в интернет. Пароль пользователя передаётся
только Firebase по HTTPS. Ktor получает ID-токен и определяет владельца
задачи по проверенному UID.
