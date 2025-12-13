# Flower Management System

This project is a backend application designed to manage flowers, their growth stages, and maintenance activities. It follows a layered architecture with a clear separation of concerns, making it easy to extend and maintain.

## Folder Structure

The project is organized as follows inside the main folder:

main/
├── model/
├── controller/
├── services/
├── repository/
└── exceptions/

### 1. `model/`

Contains the objects and enums that define the core data structures of the application.

- **Objects**
  - `Flower` – Represents a flower entity.
  - `Growth` – Represents the growth information of a flower.
  - `Maintenance` – Represents maintenance activities for a flower.

- **Enums**
  - `GrowthStage` – Enum describing the different stages of flower growth.
  - `FlowerColor` – Enum describing possible flower colors.
  - `MaintenanceType` – Enum describing types of maintenance activities.

### 2. `controller/`

Currently empty, but this layer will be responsible for handling incoming HTTP requests and routing them to the appropriate services.

### 3. `services/`

Currently empty, but this layer will contain the business logic of the application. Services will act as an intermediary between controllers and repositories.

### 4. `repository/`

Contains the classes responsible for interacting with the database:

- `FlowerRepository`
- `GrowthRepository`
- `MaintenanceRepository`

Repositories provide data access methods that will be called by the service layer.

### 5. `exceptions/`

Currently empty, but this folder will be used to define custom exceptions to handle error scenarios in the application.
