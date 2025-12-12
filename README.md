#  <img width="50" height="50" alt="image-Photoroom" src="https://github.com/user-attachments/assets/fd03b99c-9018-4b78-ac57-dca02205e749" /> StartupHeroes – Package Kafka Producer Case

Bu proje, PostgreSQL'deki Package kayıtlarını işleyip Kafka'ya yayımlayan bir Spring Boot uygulamasıdır. 

##  Proje Kapsamı

- Kafka Producer geliştirme
- Kafka Topic mesaj yapısı
- MapStruct ile DTO dönüşümü
- JPA/Hibernate + PostgreSQL
- Liquibase ile schema & seed veri yönetimi
- Docker Compose ile çoklu servis orkestrasyonu
- Embedded Kafka ile entegrasyon testleri
- OpenAPI/Swagger ile API dokümantasyonu
- Kafka UI ile mesaj inceleme

---


##   Projenin Kurulumu

###  1. Projeyi Klonla
```bash
git clone https://github.com/ADBERILGEN35/spring-kafka.git
```
```bash
cd spring-kafka
```
###  2. Testleri Çalıştır

```bash
chmod +x gradlew
```
```bash
./gradlew clean test
```

Bu testler aşağıdaki kritik akışları kapsar:

| Test | Açıklama |
|------|----------|
| `shouldSendSinglePackageToKafka` | Tek paketin Kafka'ya gönderilmesi |
| `shouldNotSendCancelledPackageToKafka` | Cancelled paketin engellenmesi (400) |
| `shouldReturnNotFoundForNonExistentPackage` | Olmayan paket sorgusu (404) |
| `shouldSendAllNonCancelledPackagesToKafka` | Bootstrap - sadece aktif paketler |
| `shouldSetNullFieldsForNonCompletedPackage` | Non-completed paketlerde null alanlar |
| `shouldRejectInvalidPackageId` | Negatif ID validation hatası |

---

###  3. Docker Compose ile Servisleri Başlat

```bash
docker compose up -d --build
```

| Servis | Açıklama | Port |
|--------|----------|------|
| app | Spring Boot mikroservisi | 8080 |
| postgres | PostgreSQL 15 | 5432 |
| kafka | Kafka broker (KRaft mode) | 9092 / 29092 |
| kafka-ui | Mesaj görüntüleme UI | 8090 |

---

###  4. Servislerin Durumunu Kontrol Et

```bash
docker compose ps
```

Tüm servislerin `healthy` durumda olduğundan emin olun.

---

## Erişim Noktaları

| Servis | URL |
|--------|-----|
| **API Base URL** | http://localhost:8080 |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs |
| **Kafka UI** | http://localhost:8090 |

### Kafka Topic Adları

| Profil | Topic Adı |
|--------|-----------|
| Docker (production) | `package-events` |
| Test | `package-events-test` |

---

##  API Kullanımı

###  Tek bir paketi Kafka'ya gönder

```bash
curl -X POST http://localhost:8080/kafka/send/19604181
```

**Başarılı Yanıt (200 OK):**
```json
{
  "success": true,
  "message": "Package sent to Kafka successfully",
  "data": 19604181,
  "timestamp": "2024-01-15T10:30:00.123456Z"
}
```

---

###  Tüm paketleri Kafka'ya gönder (bootstrap)

```bash
curl -X POST http://localhost:8080/kafka/bootstrap
```

**Başarılı Yanıt (200 OK):**
```json
{
  "success": true,
  "message": "All packages sent successfully",
  "data": 7,
  "timestamp": "2024-01-15T10:30:00.123456Z"
}
```

> **Not:** Veritabanında 8 kayıt vardır ancak 1 tanesi `cancelled=true` olduğu için sonuç **7 kayıt** döner.

---

###  Hata Yanıtları

#### Package Bulunamadı (404 Not Found)

```bash
curl -X POST http://localhost:8080/kafka/send/999999
```

```json
{
  "success": false,
  "message": "Package not found with id: 999999",
  "timestamp": "2024-01-15T10:30:00.123456Z"
}
```

#### Cancelled Package (400 Bad Request)

```bash
curl -X POST http://localhost:8080/kafka/send/19604182
```

```json
{
  "success": false,
  "message": "Package is cancelled and cannot be sent to Kafka. Id: 19604182",
  "timestamp": "2024-01-15T10:30:00.123456Z"
}
```

#### Geçersiz Package ID (400 Bad Request)

```bash
curl -X POST http://localhost:8080/kafka/send/-1
```

```json
{
  "success": false,
  "message": "must be greater than 0",
  "timestamp": "2024-01-15T10:30:00.123456Z"
}
```

---

##  Veri Modeli

### Package Entity Alanları

| Alan | Tip | Açıklama |
|------|-----|----------|
| id | Long | Primary key |
| createdAt | LocalDateTime | Paket oluşturulma zamanı |
| completedAt | LocalDateTime | Paket tamamlanma zamanı |
| pickedUpAt | LocalDateTime | Paket toplama zamanı |
| inDeliveryAt | LocalDateTime | Teslimat başlangıç zamanı |
| eta | Integer | Tahmini teslimat süresi (dakika) |
| status | Enum | WAITING_FOR_ASSIGNMENT, COLLECTED, PICKED_UP, IN_DELIVERY, COMPLETED, CANCELLED |
| cancelled | Boolean | İptal durumu |

### MappedPackage DTO Alanları

| Alan | Tip | Açıklama |
|------|-----|----------|
| id | Long | Package ID |
| createdAt | String | Format: `yyyy-MM-dd HH:mm:ss.SSSSSS` |
| lastUpdatedAt | String | Format: `yyyy-MM-dd HH:mm:ss.SSSSSS` |
| collectionDuration | Integer | Toplama süresi (dakika) |
| deliveryDuration | Integer | Teslimat süresi (dakika) |
| eta | Integer | Tahmini süre (dakika) |
| leadTime | Integer | Toplam süre (dakika) |
| orderInTime | Boolean | Zamanında teslim edildi mi? |

### Süre Hesaplamaları

| Alan | Hesaplama | Açıklama |
|------|-----------|----------|
| **leadTime** | `createdAt` → `completedAt` | Toplam işlem süresi |
| **collectionDuration** | `createdAt` → `pickedUpAt` | Paket toplanana kadar geçen süre |
| **deliveryDuration** | `inDeliveryAt` → `completedAt` | Teslimat süresi |
| **orderInTime** | `leadTime ≤ eta` | Zamanında teslim kontrolü |

---

### Durumlara Göre Davranış

####  COMPLETED Olmayan Paketler

Aşağıdaki alanlar **null** olarak döner:

- `collectionDuration`
- `deliveryDuration`
- `leadTime`
- `orderInTime`

####  CANCELLED Paketler

| Endpoint | Davranış |
|----------|----------|
| `/kafka/send/{id}` | **400 Bad Request** hatası döner |
| `/kafka/bootstrap` | Listeye dahil edilmez, Kafka'ya gönderilmez |

---

##  Kafka Topic Schema

### Topic Bilgileri

| Özellik | Değer |
|---------|-------|
| Topic Adı (Docker) | `package-events` |
| Topic Adı (Test) | `package-events-test` |
| Partition Sayısı | 1 |
| Replica Sayısı | 1 |

### Mesaj Yapısı

| Bileşen | Tip | Açıklama |
|---------|-----|----------|
| **Key** | String | Package ID |
| **Value** | JSON | MappedPackage nesnesi |

### Örnek Kafka Mesajı (COMPLETED Package)

**Key:** `19604181`

**Value:**
```json
{
  "id": 19604181,
  "createdAt": "2021-11-13 10:47:52.675248",
  "lastUpdatedAt": "2021-11-13 11:40:15.314340",
  "collectionDuration": 2,
  "deliveryDuration": 34,
  "eta": 277,
  "leadTime": 52,
  "orderInTime": true
}
```

> **Hesaplama Detayı (Sample 1):**
> - `collectionDuration`: 10:47:52 → 10:49:50 = **2 dakika**
> - `deliveryDuration`: 11:05:56 → 11:40:15 = **34 dakika**
> - `leadTime`: 10:47:52 → 11:40:15 = **52 dakika**
> - `orderInTime`: 52 ≤ 277 = **true**

### Örnek Kafka Mesajı (IN_DELIVERY Package)

**Key:** `19604183`

**Value:**
```json
{
  "id": 19604183,
  "createdAt": "2021-11-13 11:50:00.000000",
  "lastUpdatedAt": "2021-11-13 12:15:00.000000",
  "collectionDuration": null,
  "deliveryDuration": null,
  "eta": 250,
  "leadTime": null,
  "orderInTime": null
}
```

### Mesaj Kuralları

-  COMPLETED paketlerde tüm süre alanları hesaplanır
-  COMPLETED olmayan paketlerde süre alanları `null` olur
-  CANCELLED paketler asla Kafka'ya gönderilmez
-  `eta` null ise `orderInTime` da `null` olur

---

##  Mimari

### Katmanlı Yapı

```
┌──────────────────────────────────────────────────────────┐
│                      Controller Layer                    │
│                    (KafkaController)                     │
├──────────────────────────────────────────────────────────┤
│                      Service Layer                       │
│    ┌──────────────────────┐    ┌───────────────────────┐ │
│    │ KafkaOperationService│─▶ │ KafkaProducerService   │ │
│    └──────────┬───────────┘    └───────────────────────┘ │
│               │                                          │
│    ┌──────────▼──────────┐     ┌───────────────────────┐ │
│    │   PackageService    │───▶│   PackageMapper        │ │
│    └──────────┬──────────┘     └───────────────────────┘ │
├───────────────┼──────────────────────────────────────────┤
│               │           Repository Layer               │
│    ┌──────────▼──────────┐                               │
│    │  PackageRepository  │                               │
│    └─────────────────────┘                               │
├──────────────────────────────────────────────────────────┤
│                    Infrastructure                        │
│         PostgreSQL              Apache Kafka             │
└──────────────────────────────────────────────────────────┘
```

## Veritabanı

### Liquibase Migrations

| Changeset ID | Açıklama |
|--------------|----------|
| `001-create-package-table` | Package tablosu ve index oluşturma |
| `002-insert-sample-data` | 8 adet örnek veri ekleme |

### Sample Data Özeti

| ID | Status | Cancelled | Kafka'ya Gönderilir? |
|----|--------|-----------|---------------------|
| 19604181 | COMPLETED | false |  Evet |
| 19604182 | CANCELLED | true |  Hayır |
| 19604183 | IN_DELIVERY | false |  Evet (null alanlarla) |
| 19604184 | WAITING_FOR_ASSIGNMENT | false |  Evet (null alanlarla) |
| 19604185 | COLLECTED | false |  Evet (null alanlarla) |
| 19604186 | PICKED_UP | false |  Evet (null alanlarla) |
| 19604187 | COMPLETED | false |  Evet (orderInTime=false) |
| 19604188 | COMPLETED | false |  Evet (eta=null) |

---



## Teknoloji Stack

| Kategori | Teknoloji | Versiyon |
|----------|-----------|----------|
| **Runtime** | Java | 21 |
| **Framework** | Spring Boot | 3.4.0 |
| **Build Tool** | Gradle | 9.2.1 |
| **Database** | PostgreSQL | 15 |
| **ORM** | Hibernate/JPA | - |
| **Migration** | Liquibase | - |
| **Messaging** | Apache Kafka | 3.7.0 |
| **Mapping** | MapStruct | 1.5.5 |
| **Validation** | Jakarta Validation | - |
| **Documentation** | SpringDoc OpenAPI | 2.8.0 |
| **Testing** | JUnit 5, Spring Kafka Test | - |
| **Containerization** | Docker, Docker Compose | - |

---


**© 2025 Startup Heroes - Package Kafka Case Study**
