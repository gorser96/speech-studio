# Архитектура проекта «Кабинет речи»

Этот документ задаёт обязательные архитектурные ориентиры для изменений OpenSpec. Перед проектированием и реализацией учитывай применимые разделы; в design.md кратко указывай затронутые слои и обоснование решений. Примеры и целевые технологии не доказывают их наличие в коде: проверяй исходники и зависимости. Не внедряй дополнительные слои попутно без необходимости.

## 1. Общая информация

«Кабинет речи» — мобильное Android-приложение для планирования и ведения профессиональной деятельности логопеда.

На этапе MVP приложение является **полностью автономным (offline-first)**:

- все основные функции доступны без подключения к интернету;
- данные хранятся локально на устройстве;
- серверная часть отсутствует;
- синхронизация с внешним сервером не является частью MVP;
- приложение не зависит от доступности сети для выполнения основных операций.

### Технологический стек

| Компонент | Технология |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose |
| Архитектура | Clean Architecture + MVVM |
| Локальная БД | Room |
| База данных | SQLite |
| Асинхронность | Kotlin Coroutines |
| Реактивные данные | Kotlin Flow |
| DI | Hilt |
| Навигация | Navigation Compose |
| Минимальная версия Android | определяется отдельно |

---

# 2. Архитектурный подход

Основной архитектурный принцип:

> UI не должен знать о структуре базы данных, а бизнес-логика не должна зависеть от Android Framework.

Проект разделяется на три основных слоя:

```text
┌─────────────────────────────────────┐
│           Presentation              │
│       Compose + ViewModel           │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│              Domain                 │
│       Entities + Use Cases          │
└──────────────────┬──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│               Data                  │
│     Repository + Room + SQLite      │
└─────────────────────────────────────┘
```

Зависимости направлены только внутрь:

```text
Presentation → Domain ← Data
```

При этом `Domain` не зависит ни от `Presentation`, ни от `Data`.

---

# 3. Почему Clean Architecture + MVVM

В «Кабинете речи» используется умеренный вариант Clean Architecture.

Цель — не создать максимально сложную архитектуру, а обеспечить:

- разделение UI и бизнес-логики;
- тестируемость;
- предсказуемую работу с локальными данными;
- возможность изменять UI независимо от БД;
- возможность изменять способ хранения данных независимо от бизнес-логики;
- отсутствие зависимости приложения от сети.

Архитектура не должна приводить к созданию большого количества абстракций ради самих абстракций.

---

# 4. Слои приложения

## 4.1 Presentation

Presentation отвечает за отображение информации и взаимодействие с пользователем.

В него входят:

- Jetpack Compose UI;
- ViewModel;
- UI State;
- навигация;
- преобразование Domain-моделей в состояние, необходимое UI.

Presentation **не работает напрямую с Room**.

### Пример

```text
ChildScreen
     ↓
ChildViewModel
     ↓
GetChildUseCase
```

ViewModel не должна обращаться напрямую к `ChildDao`.

Неправильно:

```kotlin
class ChildViewModel(
    private val dao: ChildDao
)
```

Правильно:

```kotlin
class ChildViewModel(
    private val getChild: GetChildUseCase
)
```

---

# 5. ViewModel

ViewModel является связующим компонентом между Compose UI и Domain layer.

Основные задачи ViewModel:

- получать действия пользователя;
- вызывать Use Case;
- хранить состояние экрана;
- преобразовывать данные Domain в UI State;
- обрабатывать состояния загрузки и ошибок.

ViewModel не должна содержать бизнес-правила приложения.

### Пример

```kotlin
class ChildViewModel(
    private val getChild: GetChildUseCase
) : ViewModel() {

    fun loadChild(id: Long) {
        viewModelScope.launch {
            getChild(id).collect { child ->
                // обновление состояния
            }
        }
    }
}
```

Бизнес-правило должно находиться в Use Case, а не внутри ViewModel.

---

# 6. UI State

Для каждого сложного экрана рекомендуется иметь отдельный класс состояния.

Например:

```kotlin
data class ChildState(
    val child: Child? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

Compose получает состояние и отображает его.

```text
ViewModel
    ↓
ChildState
    ↓
Compose
```

UI не должен самостоятельно получать данные из Repository или Database.

---

# 7. Domain Layer

Domain — центральный слой приложения.

Он содержит бизнес-сущности и бизнес-операции.

Domain не должен импортировать:

- `android.*`;
- Room;
- Compose;
- Retrofit;
- конкретные классы базы данных.

Структура:

```text
domain/
├── model/
└── usecase/
```

---

# 8. Domain Models

Domain Model описывает сущности предметной области.

Например:

```kotlin
data class Child(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val birthDate: LocalDate?,
    val notes: String?
)
```

Это **не Room Entity**.

Domain-модель не должна содержать аннотации Room:

```kotlin
@Entity
```

или:

```kotlin
@ColumnInfo
```

---

# 9. Use Cases

Use Case представляет одну законченную бизнес-операцию.

Примеры:

```text
CreateChildUseCase
UpdateChildUseCase
DeleteChildUseCase
GetChildUseCase
GetChildrenUseCase

CreateLessonUseCase
UpdateLessonUseCase
DeleteLessonUseCase
GetLessonsForDateUseCase

AttachVideoUseCase
DetachVideoUseCase

CalculateReportUseCase
```

Use Case должен быть небольшим и выполнять одну понятную операцию.

Пример:

```kotlin
class CreateChildUseCase(
    private val repository: ChildRepository
) {
    suspend operator fun invoke(child: Child) {
        repository.create(child)
    }
}
```

---

# 10. Repository

Repository является абстракцией доступа к данным.

Интерфейс Repository располагается в Domain layer.

Например:

```kotlin
interface ChildRepository {

    fun observeChildren(): Flow<List<Child>>

    suspend fun getById(id: Long): Child?

    suspend fun create(child: Child)

    suspend fun update(child: Child)

    suspend fun delete(id: Long)
}
```

Domain знает только этот интерфейс.

Он не знает, откуда берутся данные.

---

# 11. Data Layer

Data Layer отвечает за фактическое хранение данных.

На этапе MVP основным источником данных является Room.

Структура:

```text
data/
├── local/
│   ├── dao/
│   ├── entity/
│   └── database/
└── repository/
```

Основные компоненты:

```text
Repository Implementation
          ↓
        DAO
          ↓
        Room
          ↓
       SQLite
```

---

# 12. Room Entity

Room Entity представляет структуру хранения данных.

Например:

```kotlin
@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String?,
    val birthDate: LocalDate?,
    val notes: String?
)
```

`ChildEntity` не используется напрямую в UI.

---

# 13. Mapping

Между Domain Model и Data Entity используется преобразование.

```text
ChildEntity
    ↓
Child
```

и обратно:

```text
Child
    ↓
ChildEntity
```

Например:

```kotlin
fun ChildEntity.toDomain(): Child =
    Child(
        id = id,
        firstName = firstName,
        lastName = lastName,
        birthDate = birthDate,
        notes = notes
    )
```

Это позволяет менять структуру БД, не изменяя Domain и UI.

---

# 14. Repository Implementation

Интерфейс:

```kotlin
interface ChildRepository
```

располагается в Domain.

Реализация:

```kotlin
class ChildRepositoryImpl(
    private val dao: ChildDao
) : ChildRepository {

    override fun observeChildren(): Flow<List<Child>> =
        dao.observeAll()
            .map { entities ->
                entities.map { it.toDomain() }
            }

    override suspend fun create(child: Child) {
        dao.insert(child.toEntity())
    }
}
```

Таким образом:

```text
Domain

ChildRepository
       ↑
       │
Data

ChildRepositoryImpl
       ↓
    ChildDao
       ↓
      Room
```

---

# 15. Room как Source of Truth

В MVP локальная база данных является единственным источником истины.

```text
                 Room
                  │
                  │
          ┌───────┴────────┐
          ↓                ↓
       Read             Write
          │                │
          └───────┬────────┘
                  ↓
             Repository
                  ↓
               UseCase
                  ↓
             ViewModel
                  ↓
               Compose
```

Изменение данных в Room должно автоматически приводить к обновлению наблюдаемых данных через `Flow`.

Например:

```kotlin
@Query("""
    SELECT * FROM lessons
    WHERE date = :date
    ORDER BY startTime
""")
fun observeLessons(date: LocalDate): Flow<List<LessonEntity>>
```

После изменения записи Room эмитит новое значение `Flow`, и UI получает актуальное состояние.

Не следует строить логику на ручных вызовах вида:

```text
save()
reload()
refresh()
```

если данные уже могут быть реактивно получены из Room.

---

# 16. Offline-first

Приложение проектируется с предположением:

> Интернет может отсутствовать всегда.

Поэтому основные функции не должны зависеть от сети.

Пользователь должен иметь возможность:

- создавать детей;
- редактировать детей;
- создавать занятия;
- редактировать занятия;
- просматривать расписание;
- работать с упражнениями;
- создавать и просматривать локальные данные;
- формировать вычисляемые отчёты

без подключения к интернету.

---

# 17. Backend

Backend отсутствует в MVP.

Не используются:

- REST API;
- авторизация через сервер;
- удалённая БД;
- синхронизация;
- облачное хранение данных.

В будущем серверная синхронизация может быть добавлена на уровне Data Layer.

Текущая архитектура:

```text
Repository
    ↓
Room
```

Возможная будущая архитектура:

```text
Repository
    ↓
┌───────────────┐
│ Sync / Remote │
└───────┬───────┘
        │
   ┌────┴────┐
   ↓         ↓
 Room      Server
```

Добавление серверной синхронизации не должно требовать изменений Presentation Layer.

---

# 18. Работа с видео

Приложение **не занимается видеосъёмкой**.

Видео создаётся средствами устройства отдельно.

«Кабинет речи» работает только с уже существующим видео:

- прикрепляет видео к соответствующей сущности;
- отображает информацию о прикреплённом видео;
- открывает видео через системные средства Android;
- позволяет открепить видео.

Сам видеофайл не является обычным полем сущности занятия.

Рекомендуется хранить ссылку/URI на видео в отдельной сущности.

Например:

```text
Lesson
   │
   └── VideoAttachment
           ├── id
           ├── lessonId
           └── uri
```

Фактическая работа с URI должна учитывать ограничения Android Storage Access Framework и разрешения на доступ к выбранному пользователем файлу.

---

# 19. Отчёты

Отчёты не являются отдельными хранимыми сущностями.

Они вычисляются на основании текущих данных.

Например:

```text
Lessons
   +
Children
   +
Other data
   ↓
CalculateReportUseCase
   ↓
ReportResult
   ↓
UI
```

Это означает, что результат отчёта не требуется сохранять в базе.

---

# 20. Навигация

Навигация является частью Presentation Layer.

Пример:

```text
Home
 ├── Schedule
 ├── Children
 │    └── Child Details
 ├── Lessons
 │    └── Lesson Details
 ├── Exercises
 └── Reports
```

Compose Navigation отвечает только за переходы между экранами.

Бизнес-логика не должна зависеть от NavController.

---

# 21. Dependency Injection

Для Dependency Injection используется Hilt.

Зависимости строятся следующим образом:

```text
ViewModel
    ↓
UseCase
    ↓
Repository
    ↓
DAO
    ↓
Room Database
```

Например:

```text
ChildViewModel
      ↓
GetChildUseCase
      ↓
ChildRepository
      ↓
ChildRepositoryImpl
      ↓
ChildDao
      ↓
AppDatabase
```

Hilt отвечает за создание и передачу этих зависимостей.

---

# 22. Структура проекта

Рекомендуемая начальная структура:

```text
app/
│
├── src/main/java/.../
│
├── core/
│   ├── database/
│   ├── navigation/
│   ├── ui/
│   └── util/
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── database/
│   │
│   └── repository/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
└── feature/
    ├── children/
    ├── lessons/
    ├── schedule/
    ├── exercises/
    └── reports/
```

Для MVP не требуется разбивать каждую feature на отдельный Gradle-модуль.

Сначала достаточно одного Android-модуля с логическим разделением пакетов.

---

# 23. Правила зависимостей

## Presentation может зависеть от:

- Domain;
- Core;
- Android Framework;
- Jetpack Compose.

## Domain может зависеть от:

- Kotlin Standard Library;
- Kotlin Coroutines/Flow при необходимости.

Domain **не должен зависеть от Android Framework и Room**.

## Data может зависеть от:

- Domain;
- Room;
- Android Framework;
- других библиотек хранения данных.

## Core

Core содержит общие технические компоненты, которые используются несколькими частями приложения.

Не следует превращать `core` в свалку вспомогательных классов.

---

# 24. Основное правило архитектуры

При добавлении новой функциональности следует двигаться в следующем направлении:

```text
1. Определить Domain Model
             ↓
2. Определить Repository Interface
             ↓
3. Создать Use Case
             ↓
4. Реализовать Data Layer
             ↓
5. Создать ViewModel
             ↓
6. Создать Compose Screen
```

Например, добавление функции создания занятия:

```text
Lesson
  ↓
LessonRepository
  ↓
CreateLessonUseCase
  ↓
LessonEntity + LessonDao
  ↓
LessonRepositoryImpl
  ↓
CreateLessonViewModel
  ↓
CreateLessonScreen
```

---

# 25. Что не следует делать

### Не обращаться к Room из Compose

Плохо:

```text
Compose → DAO
```

### Не обращаться к DAO из ViewModel

Плохо:

```text
ViewModel → DAO
```

### Не помещать бизнес-логику в ViewModel

Плохо:

```kotlin
if (lesson.duration > 45) {
    ...
}
```

если это является бизнес-правилом приложения.

Лучше:

```text
ViewModel
   ↓
UseCase
   ↓
Business Rule
```

### Не использовать Room Entity в UI

Плохо:

```text
Compose → ChildEntity
```

Правильно:

```text
Compose → Child
```

### Не создавать Repository без необходимости

Если операция тривиальна и уже полностью покрывается существующей абстракцией, не нужно создавать дополнительные уровни только ради соответствия шаблону.

---

# 26. Тестирование

Архитектура должна позволять проверять правила независимо, но не требует теста на каждый слой или класс.
Выбирай минимальный набор, достаточный для риска изменения и критериев приёмки.

- Сначала используй существующие адресные проверки. Добавляй тест для непокрытого существенного правила, регрессии или риска потери данных.
- Проверяй поведение на самом дешёвом достаточном уровне: чистую логику локально, Room/миграции — при реальной зависимости от БД, UI — только для существенного риска взаимодействия.
- Не дублируй одинаковые сценарии в DAO, repository, ViewModel и Compose; не проверяй тривиальное делегирование и детали реализации.
- Миграции, атомарность записи и сохранность данных требуют подходящей проверки, даже если она дороже обычного unit-теста.
- Для простой визуальной правки допустима адресная ручная проверка. Обязательного TDD и порога покрытия нет.
- Существующие полезные тесты сохраняются; сокращение набора требует конкретного доказательства дублирования или устаревания.

---

# 27. Пример полного сценария

Пользователь создаёт занятие.

```text
┌──────────────────────┐
│  CreateLessonScreen  │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  LessonViewModel     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ CreateLessonUseCase  │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ LessonRepository     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ LessonRepositoryImpl │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│      LessonDao       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│        Room          │
└──────────────────────┘
```

После сохранения:

```text
Room
 ↓
Flow
 ↓
Repository
 ↓
ViewModel
 ↓
Compose
 ↓
UI автоматически обновляется
```

---

# 28. Ключевой принцип проекта

Архитектура «Кабинета речи» строится вокруг локальных данных.

```text
                    ┌───────────────┐
                    │    Compose    │
                    └───────┬───────┘
                            │
                     Presentation
                            │
                    ┌───────▼───────┐
                    │   ViewModel   │
                    └───────┬───────┘
                            │
                    ┌───────▼───────┐
                    │    UseCase    │
                    └───────┬───────┘
                            │
                       Domain
                            │
                    ┌───────▼───────┐
                    │  Repository   │
                    └───────┬───────┘
                            │
                         Data
                            │
                    ┌───────▼───────┐
                    │     Room      │
                    └───────────────┘
```

**Главное архитектурное решение MVP:**

> «Кабинет речи» — локальное Android-приложение. Room является источником истины, Domain содержит бизнес-логику, ViewModel управляет состоянием экранов, а Compose отвечает за отображение.

Сеть, backend и синхронизация не являются частью текущей архитектуры приложения.
