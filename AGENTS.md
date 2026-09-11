# Paradox Android App — Agent Rules

You are working on Paradox.

Follow these rules:

1. Do not change folder structture
2. Do work Evnironment variable driven
3. Do not push .env file
4. Do not access .env file
5. Do not hardcode anything
6. Ask for permission wherever required
7. Before starting any work give me a call "Roger That"
   and when the work is done give me call "Over n Out"
8. Whenever i say "bye" you have to commit and push everything and update progress file and technical file
   and also store everything from that session to your own repo memory and after its done say "Signing off".

## Additional Project Rules

### Android Development

* Build the application as a native Android application.
* Follow the Android architecture and project structure defined by the project documentation.
* Do not introduce unnecessary frameworks, libraries, or dependencies.
* Use Kotlin for Android development unless the project explicitly requires otherwise.
* Follow modern Android development practices.
* Keep Android-specific implementation inside the appropriate project layers and packages.
* Do not change the existing project architecture without permission.

### UI Development

* Follow the project's Design System for colors, typography, spacing, shapes, components, and other visual decisions.
* Use reusable UI components instead of duplicating UI code.
* Do not use hardcoded colors, dimensions, typography values, or other design values when they are defined by the Design System.
* Do not use unnecessary one-off UI implementations.
* Keep UI consistent across all screens.
* Support different Android screen sizes and orientations where required.
* Implement loading, empty, error, and success states where required.
* Use animations and micro-interactions only when they improve usability or feedback.
* Follow Android accessibility guidelines.
* Support appropriate touch targets, content descriptions, and readable UI.
* Respect system settings such as font scaling and reduced-motion preferences where applicable.

### Jetpack Compose

* Use Jetpack Compose when Compose is the project's approved UI framework.
* Keep Composable functions focused and reusable.
* Do not place business logic directly inside Composable UI.
* Keep UI state predictable and properly scoped.
* Use appropriate state-management patterns defined by the project architecture.
* Avoid unnecessary recompositions.
* Do not create duplicate Composable implementations for the same reusable UI element.

### Architecture

* Follow the architecture defined by the project documentation.
* Keep UI, business logic, and data responsibilities separated.
* Do not put business logic directly inside Activities, Fragments, or Composables.
* Use ViewModels for screen-level UI state and logic where required by the architecture.
* Keep data access inside the appropriate data layer.
* Use repositories or the project's defined data-access abstraction where required.
* Do not bypass established architectural layers without a clear reason.
* Keep classes, functions, and components focused and reasonably small.

### State & Data

* Use a single, clear source of truth for application data where applicable.
* Handle loading, success, empty, and error states explicitly.
* Do not silently ignore errors.
* Do not use fake, demo, or placeholder user data unless explicitly requested.
* Do not store sensitive information insecurely.
* Use appropriate Android storage mechanisms based on the type and sensitivity of the data.
* Do not persist data unnecessarily.

### Local Database

* Use the database technology defined by the Android project documentation.
* Follow the project's database schema and data model.
* Do not manually modify database files or schema outside the defined database mechanism.
* Handle database migrations properly when the schema changes.
* Do not introduce duplicate or unnecessary data.
* Do not seed fake or demo financial data unless explicitly requested.

### Networking

* Follow the networking architecture defined by the project.
* Keep network communication inside the appropriate data/service layer.
* Do not make network calls directly from UI components.
* Follow the API contracts defined by the project documentation when an API is used.
* Do not invent undocumented endpoints, parameters, or response formats.
* Handle network failures, timeouts, and unavailable connections properly.
* Do not expose API keys, tokens, or other secrets in source code.

### Security

* Never hardcode secrets, credentials, API keys, tokens, or passwords.
* Never commit `.env`, `.env.local`, or other files containing real secrets.
* Do not expose sensitive information through logs.
* Do not store sensitive authentication information in insecure storage.
* Use Android's recommended secure storage mechanisms where required.
* Request only the permissions actually required by the application.
* Do not bypass Android security restrictions.

### Configuration

* Keep configuration values outside source code when they should be configurable.
* Use environment/configuration mechanisms defined by the project.
* Do not hardcode URLs, API keys, secrets, feature configuration, or environment-specific values.
* Keep development, testing, and production configuration appropriately separated.
* Do not access `.env` files when prohibited by the project rules.

### Permissions

* Request Android permissions only when they are required.
* Request permissions at the appropriate time instead of requesting unnecessary permissions at application startup.
* Explain permission requirements to the user when appropriate.
* Gracefully handle permission denial.
* Never attempt to bypass Android permission restrictions.

### Testing

* Add tests for important business logic and critical application flows.
* Test ViewModels and important state-management logic where applicable.
* Test validation and important edge cases.
* Test database operations where required.
* Test important UI behavior where required.
* Do not consider a feature complete when required tests are missing.
* Do not rely only on manual testing for critical functionality.

### Performance

* Avoid unnecessary work on the main thread.
* Do not perform heavy operations directly on the UI thread.
* Avoid unnecessary database, network, or resource operations.
* Avoid memory leaks.
* Use appropriate lifecycle-aware components.
* Optimize only when there is a real need; do not add unnecessary complexity.

### Resources

* Use Android resources appropriately for strings, dimensions, drawables, and other reusable values.
* Do not hardcode user-visible strings directly in UI code when they should be localized.
* Keep resource naming consistent.
* Avoid duplicate resources.
* Follow the project's resource organization.

### Code Quality

* Keep code readable, maintainable, and consistent with the existing project.
* Prefer reusable components and utilities over repeated code.
* Avoid unnecessary abstraction.
* Avoid unnecessary dependencies.
* Remove unused code and imports.
* Do not leave debugging code, temporary implementations, or unnecessary comments in production code.
* Do not change unrelated code while implementing a feature.

### Scope & Changes

* V1 must follow the approved PRD and SRS scope.
* Do not implement future-phase features "just in case."
* Do not introduce unnecessary complexity.
* Do not change the existing architecture, folder structure, or project rules without a clear requirement.
* Do not remove or modify Rules 1–8.
* Do not remove or modify existing project rules unless explicitly instructed.
* Before making a significant change, verify it against the PRD, SRS, and Design System.
* If a requirement is unclear or conflicts with existing documentation, ask for permission before making a significant decision.
