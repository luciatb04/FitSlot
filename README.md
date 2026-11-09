# FitSlot

**Sports centre management system · Agile Software Development project**

FitSlot is an academic software project developed by the **Duff Tech** team for the *Agile Software Development* course. Its goal is to help a sports centre manage approximately **900 members**, a weight room, two class rooms and a swimming pool through a single application.

The centre currently relies on paper forms, membership cards, spreadsheets and phone bookings. FitSlot aims to centralise memberships, class reservations, waiting lists, access checks, instructor scheduling and management reports, reducing booking disputes and administrative work.

> **Project status:** This README describes the **target product defined by the project brief**, not a claim that every feature has already been implemented. The repository starts from an existing Spring Boot + React teaching template and will be adapted incrementally through the course sprints.

## 1. Project objectives

- Manage members, contact details, medical certificate validity and membership history.
- Configure membership plans with different services, prices, durations and time restrictions.
- Publish recurring fitness classes and manage reservations with limited capacity.
- Maintain waiting lists and assign newly available places fairly.
- Validate access to facilities and classes at reception.
- Manage instructors' weekly availability, class assignments and last-minute replacements.
- Notify affected members when their class is cancelled or its instructor changes.
- Give management visibility into attendance, class occupancy, membership trends and revenue.

## 2. Users and roles

| Role | Main responsibilities |
| --- | --- |
| **Reception staff** | Register members, renew memberships, record payments, check medical certificates and validate entrances. |
| **Member** | View their membership and certificate status, explore the timetable, book or cancel classes and check waiting-list status. |
| **Instructor** | Declare weekly availability, view assigned classes and booked participants, and report absences. |
| **Manager** | Define membership plans and timetables, assign or replace instructors, manage cancellations and review operational statistics. |

These are the **FitSlot business roles**. Authentication and role mappings provided by the starter project may need adaptation to match them.

## 3. Functional scope

### Member and medical certificate management

The system should maintain a member profile with personal and contact information, the medical certificate's issue and expiry dates, and a history of memberships. An expired certificate must prevent access even if the membership is otherwise valid.

### Membership plans and subscriptions

The centre offers several membership types:

| Membership type | Key conditions |
| --- | --- |
| **Monthly** | Valid for a monthly period; access depends on the services included in the selected plan. |
| **Yearly** | Valid for an annual period; access depends on the services included in the selected plan. |
| **10-entry** | Access is limited by the number of remaining entries. |
| **Student / reduced** | Reduced plan with **morning-only** access. |

Each plan must define its price, duration, included facilities or services (weight room, classes and/or swimming pool), allowed time bands and, where applicable, remaining entries. Memberships may be frozen for a documented injury for **up to 30 days per year**.

### Class timetable and reservations

Classes such as yoga, pilates, spinning and aqua aerobics are scheduled on a recurring basis, with a room, day, start time, instructor and maximum capacity. Members can reserve or cancel a place and view their booking status.

If a class is full, the member joins a waiting list. When a confirmed participant cancels, the **first eligible member in the queue** should receive the available place. The brief specifies queue order, while exceptional eligibility cases need clarification with the Product Owner.

### Access control

Reception records member entrances. Access is rejected, with a clear reason, when any applicable rule is violated:

- The membership has expired or is otherwise inactive.
- The medical certificate has expired.
- The requested service is not included in the plan.
- The visit falls outside the plan's permitted time band.
- An entry-based plan has no usable entries remaining.

### Instructor scheduling and notifications

Instructors declare weekly availability and see their assigned sessions and reservations. The manager can assign replacements or cancel classes when an instructor becomes unavailable. Affected members must be informed of changes.

### Statistics and reporting

The manager should be able to review entrances by day and time band, class occupancy, under-attended classes, membership trends and revenue.

## 4. Core business rules

| ID | Rule |
| --- | --- |
| BR-01 | Each membership grants access only to its included services and permitted time bands. |
| BR-02 | Student/reduced memberships are restricted to mornings. |
| BR-03 | Entry-based memberships track the remaining number of entries. |
| BR-04 | Access must be blocked when the medical certificate has expired. |
| BR-05 | A class cannot exceed its maximum confirmed capacity. |
| BR-06 | Once a class is full, additional requests are placed on a waiting list. |
| BR-07 | When a place is freed, waiting-list promotion follows queue order, subject to eligibility rules to be confirmed. |
| BR-08 | Cancelling a booking **less than two hours before the class** causes the loss of an entry, as stated in the brief. The effect on non-entry-based plans requires clarification. |
| BR-09 | An injury-related membership freeze requires a supporting certificate and is limited to 30 days per year. |
| BR-10 | Members affected by an instructor replacement or class cancellation must be notified. |

### Representative scenario

Marco has a 10-entry membership with two entries remaining. He attempts to book an 18:30 yoga class with 20 places, all already occupied, and is placed **third on the waiting list**. His medical certificate expires the following day. Later, a confirmed member cancels and the instructor reports an illness shortly before the class.

This scenario is used to validate waiting-list order, access eligibility, instructor changes, notifications and entry accounting. **The specification does not fully determine every outcome**; uncertain decisions must be discussed with the lecturer acting as Product Owner.

### Questions to clarify with the Product Owner

- When exactly is an entry deducted: at booking, check-in, attendance or another event?
- How does the late-cancellation penalty apply to monthly and yearly plans?
- Can a member with an expired certificate keep a booking or be promoted from the waiting list, even though entry is blocked?
- What happens to reservations and deducted entries when the centre cancels a class?
- How are membership freeze days counted and applied to expiry dates?
- What notification channel is expected, and what happens if a notification cannot be delivered?
- What minimum attendance makes a class worth running, and who may cancel it?

These are **open domain questions**, not extra rules assumed to be implemented.

## 5. Technologies and versions

The following technology stack and versions are retained from the starter repository.

| Component | Version | Notes |
| --- | --- | --- |
| Java | 21 | Target language and bytecode version. |
| JDK | 21 or 25 | Maven compiles with `--release 21` in both cases. |
| Maven | 3.9.16 | Included through Maven Wrapper. |
| Spring Boot | 3.5.16 | Backend and REST API. |
| Node.js | 24.x | Maven uses Node 24.16.0 in the `frontend` profile. |
| npm | 11 or 12 | Maven uses npm 12.0.2 in the `frontend` profile. |
| React | 18.3.1 | User interface. |
| Vite | 8.2.2 | Development server and production build. |
| Vitest | 5.0.0 | Frontend tests. |

The backend uses **Spring Boot REST APIs and JWT authentication**; the frontend is a **React single-page application**. The current starter includes an H2 development database and an optional MySQL configuration. 

> **Delivery-format decision:** The FitSlot brief asks for a **desktop application with a graphical interface and DBMS**. This starter implements a browser-based frontend. Confirm with the Product Owner whether this form is acceptable or whether a desktop delivery mechanism will be required. No additional technology or dependency is assumed here.

## 6. Requirements

- Git.
- JDK 21 or JDK 25 (JDK 21 is recommended for a shared team environment).
- Node.js 24 and npm 11 or 12 for direct frontend development.
- An IDE with Java, Maven and React support.

The Maven `frontend` profile downloads its own Node and npm distributions into `target/`. Global Node and npm are therefore not required for Maven-integrated builds, but they are required to work directly inside `frontend/`.

Verify the environment:

```bash
java --version
node --version
npm --version
./mvnw --version
```

On Windows PowerShell, check Maven with:

```powershell
.\mvnw.cmd --version
```

The Maven output should report Java 21 or Java 25. The property `maven.compiler.release=21` ensures Java 21 bytecode compatibility.

## 7. Getting the project

Clone the **team's FitSlot repository** (replace the placeholder with its actual URL):

```bash
git clone <FITSLOT_REPOSITORY_URL> fitslot
cd fitslot
./mvnw clean verify -Pfrontend
```

Windows PowerShell:

```powershell
git clone <FITSLOT_REPOSITORY_URL> fitslot
Set-Location fitslot
.\mvnw.cmd clean verify -Pfrontend
```

The full verification compiles and tests the backend, installs frontend dependencies using `npm ci`, runs the frontend tests, builds the Vite bundle and generates test reports.

Frontend integration is opt-in:

```bash
# Backend only
./mvnw clean install

# Backend and frontend
./mvnw clean install -Pfrontend
```

## 8. Local development

Run the backend and frontend in separate terminals for fast frontend reloads.

### Start the backend

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs at <http://localhost:8080>.

### Start the frontend

```bash
cd frontend
npm ci
npm run dev
```

The frontend runs at <http://localhost:5173>. Vite proxies `/api` and `/v3/api-docs` to Spring Boot on port 8080. Rerun `npm ci` after a change to `package-lock.json`.

### Useful local URLs

| Resource | URL |
| --- | --- |
| Frontend development server | <http://localhost:5173> |
| Packaged application | <http://localhost:8080> |
| Swagger UI | <http://localhost:8080/swagger-ui/index.html> |
| OpenAPI specification | <http://localhost:8080/v3/api-docs> |
| H2 console | <http://localhost:8080/h2-console> |

For the starter H2 setup: JDBC URL `jdbc:h2:mem:testdb`, username `sa`, empty password. These settings are for **local development only**.

## 9. Building the complete application

```bash
./mvnw clean package -Pfrontend
java -jar target/spring-gym-2.2.0.BUILD-SNAPSHOT.jar
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean package -Pfrontend
java -jar target/spring-gym-2.2.0.BUILD-SNAPSHOT.jar
```

The JAR name above is inherited from the starter configuration and should be updated in this README **only if the Maven artifact is renamed in `pom.xml`**. Vite builds into `frontend/dist/`, and Maven copies the result into `target/classes/static/` before packaging. The packaged application is served on port 8080.

## 10. Testing and quality

Backend tests:

```bash
./mvnw test
```

Backend and frontend tests:

```bash
./mvnw test -Pfrontend
```

Complete verification and reports:

```bash
./mvnw clean verify -Pfrontend
```

Frontend-only commands:

```bash
cd frontend
npm test
npm run test:watch
npm run coverage
npm run build
```

Generated outputs inherited from the starter include:

| Path | Purpose |
| --- | --- |
| `target/site/jacoco/` | Backend coverage report. |
| `target/site/test-status/` | Allure test status report. |
| `docs/deliverables/D3/coverage/` | Copied backend coverage report, when generated by the build. |
| `docs/deliverables/D3/status/` | Copied test status report, when generated by the build. |
| `frontend/dist/` | Frontend production bundle. |

As FitSlot is developed, automated tests should cover business cases such as expired certificates, restricted access periods, full classes, waiting-list promotions, late cancellations and instructor replacement.

## 11. Database and development data

The default Spring Boot profile uses **in-memory H2**. The starter database is seeded from `src/main/resources/data.sql` and recreated at restart with `spring.jpa.hibernate.ddl-auto=create-drop`.

An optional MySQL profile is configured in `src/main/resources/application-mysql.properties`; the supplied `docker-compose.yml` can be used for local database setup when needed.

**Important:** Existing seed data and demo accounts belong to the starter application. Their presence does **not** mean FitSlot's member, reception, instructor and manager workflows are fully implemented. Replace or adapt seed data and role mappings as those features are developed.

Do not commit production passwords, database credentials or JWT secrets. The JWT key in the starter configuration is intended only for development and must be overridden in any deployed environment.

## 12. Repository structure

```text
frontend/                 React application, Vite configuration, tests
src/main/java/            Spring Boot application and REST API
src/main/resources/       Application configuration and seed data
src/test/java/            Backend tests
src/test/jmeter/          Performance test plan
docs/                     Analysis, diagrams and course deliverables
pom.xml                   Maven build and frontend profile
docker-compose.yml        Optional local database setup
```

## 13. Agile development process

The project is developed incrementally using the course's Agile Software Development framework:

1. **Understand the domain:** identify business rules, stakeholder conflicts, exceptions and open questions for the Product Owner.
2. **Build a Product Backlog:** group user stories, technical tasks and investigation spikes into epics; prioritise them and explain dependencies.
3. **Estimate work:** use Planning Poker with **1, 2, 3, 5, 8 and 13 story points**; split items larger than 13.
4. **Define acceptance criteria:** use **Given / When / Then**, particularly for stories selected for the first sprints.
5. **Plan and review sprints:** maintain a Sprint Backlog, demonstrate working increments, collect feedback and adapt the backlog.

User stories should follow **INVEST**: Independent, Negotiable, Valuable, Estimable, Small and Testable. The lecturer acts as the **Product Owner**, including clarifying and changing requirements throughout the project.

Course documentation and sprint deliverables should be kept under `docs/` as the project evolves. Content inherited from the starter must be reviewed before being presented as FitSlot documentation.

## 14. Troubleshooting

**Wrong Java version:** Check `JAVA_HOME`, the IDE JDK and `./mvnw --version` (or `.\mvnw.cmd --version` on Windows).

**Node/npm version mismatch:** Use the versions pinned by `.nvmrc` and `.node-version` (Node 24.16.0 in the starter).

**Inconsistent frontend dependencies:** Run `npm ci` in `frontend/` rather than manually repairing `node_modules`.

**Frontend cannot contact the API:** Check that Spring Boot runs on port 8080 and Vite on 5173, and confirm the configured proxy routes.

**Missing FitSlot features:** Remember that the initial repository contains a teaching template. Consult the Product Backlog and sprint deliverables to see what has actually been implemented.

---

*FitSlot is an academic project. Product requirements are derived from the P3 · FitSlot project brief; implementation and scope are refined during the sprints with the Product Owner.*
