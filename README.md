# Планировщик задач — Android-клиент

Рабочая копия проекта `CleanArchitectureNoteApp`. Оригинал находится в
`/Users/badgraw/AndroidStudioProjects/CleanArchitectureNoteApp` и не изменён.

## Состояние

Первый этап подготовки завершён: обновлена сборка, проект и пакет названы
`TaskPlannerClient` и `com.taskplanner.app`, отображаемое название приложения —
«Планировщик задач». Экраны пока сохраняют функциональность заметок; их
переделка в задачи относится к следующим этапам.

## Сборка

Откройте эту папку в Android Studio или выполните `./gradlew :app:assembleDebug`.
Нужны JDK 17 или новее и Android SDK с платформой API 35. Локальный путь к
Android SDK указывается в `local.properties`; файл не добавляется в Git.

Совместимые версии основы: Gradle 8.13, Android Gradle Plugin 8.11.1,
Kotlin 2.2.21, Compose BOM 2025.08.00, Hilt 2.58 и Room 2.8.3.
