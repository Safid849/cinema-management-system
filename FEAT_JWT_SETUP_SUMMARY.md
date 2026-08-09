# Résumé des fichiers créés et modifiés depuis le merge de feat/security-jwt-setup

Ce document récapitule, en français, les fichiers ajoutés et modifiés dans la branche preprod depuis le commit de merge lié à la mise en place JWT (merge commit 9e9308e2). Il couvre les principaux ajouts fonctionnels, migrations, contrôleurs, services, DTOs, mappers, exceptions et tests.

## Résumé rapide
- Objectif principal : ajout et intégration du support JWT + sécurité (rôles, protection d'API), validation des DTOs, renforcement des contraintes métier en base, et ajout massif de contrôleurs, services, mappers, DTOs et tests.
- Effets notables : création d'un bootstrap pour un compte MANAGER à la première exécution, nouvelles migrations Flyway (V2, V3), gate JaCoCo (>= 80% sur **/cinema/**), CI enrichi pour tests d'intégration et upload rapport.

## Fichiers créés (sélection par catégorie)

- Configuration / CI / Docs
  - .github/workflows/ci.yml (modifié)
  - README.md (remplacé / enrichi)
  - docker-compose.yml (modifié : variables JWT + bootstrap)
  - src/main/resources/application.properties (ajout de props JWT + bootstrap)

- BDD / migrations
  - src/main/resources/db/migration/V2__integrity_and_indexes.sql (nouveau)
  - src/main/resources/db/migration/V3__business_constraints.sql (nouveau)

- Bootstrap / sécurité / JWT
  - src/main/java/com/example/demo/cinema/config/ManagerBootstrap.java (nouveau)
  - src/main/java/com/example/demo/cinema/security/JwtService.java (modifié : ajout claim userId, getters, utilitaires)
  - src/main/java/com/example/demo/cinema/security/SecurityConfig.java (modifié : règles d'autorisation)
  - src/main/java/com/example/demo/cinema/security/ReservationAccessPolicy.java (modifié)
  - src/main/java/com/example/demo/cinema/security/CinemaUserPrincipal.java (ajustement mineur)

- Contrôleurs (nouveaux / modifs)
  - src/main/java/com/example/demo/cinema/controller/AuthController.java (nouveau)
  - src/main/java/com/example/demo/cinema/controller/RoomController.java (nouveau)
  - src/main/java/com/example/demo/cinema/controller/SeatController.java (nouveau)
  - src/main/java/com/example/demo/cinema/controller/UserController.java (nouveau)
  - Modifications notables : MovieController, ProjectionController, ReservationController (ajouts de routes, validation @Valid, méthodes DELETE/GET/PUT)

- Services (nouveaux / modifiés)
  - src/main/java/com/example/demo/cinema/service/UserService.java (nouveau)
  - src/main/java/com/example/demo/cinema/service/RoomService.java (nouveau)
  - src/main/java/com/example/demo/cinema/service/SeatService.java (nouveau)
  - src/main/java/com/example/demo/cinema/service/MovieService.java (modifié : upsert et validation durée)
  - src/main/java/com/example/demo/cinema/service/ProjectionService.java (modifié : upsert, vérification chevauchement, available seats)
  - src/main/java/com/example/demo/cinema/service/ReservationService.java (modifié : upsert, suppression, vérifications de conflits)

- DTOs / mappers / exceptions
  - DTOs ajoutés/modifiés : LoginRequestDTO, LoginResponseDTO, MovieInputDTO, ProjectionInputDTO, ReservationInputDTO, ReservationDTO, UpdateUserRoleDTO, UserCreateDTO, etc.
  - Mappers ajoutés : RoomMapper, SeatMapper, UserMapper ; ReservationMapper modifié (calcul totalPrice)
  - Exceptions ajoutées : ConflictException, EmailAlreadyExistsException, InvalidCredentialsException
  - GlobalExceptionHandler modifié (normalisation des erreurs, validation détaillée)

- Repositories (méthodes ajoutées)
  - ProjectionRepository (findByRoomId, findByMovieId)
  - ReservationRepository (findByUserId, findBookedSeatIds)
  - RoomRepository (findByNumber)
  - SeatRepository (findByRoomId, findByRoomIdOrderByNumberAsc)

- Tests (importants ajouts)
  - nombreux tests unitaires et d'intégration sous src/test/java (AuthIT, ReservationFlowIT, SecurityMatrixIT, MovieServiceTest, ProjectionServiceTest, ReservationServiceTest, RoomServiceTest, SeatServiceTest, UserServiceTest, JwtServiceTest, etc.)
  - utilitaire de tests : src/test/java/.../TestFixtures.java
  - src/test/resources/application.properties (nouveau pour les tests)

- Autres
  - build.gradle (modifié : dépendances, JaCoCo coverage gate 80% sur **/cinema/**)
  - scripts de formatage et google-java-format.jar présents

## Fichiers modifiés (principaux points)
- README.md : documentation d'usage, endpoints, authentification JWT, règles métier.
- build.gradle : ajout dépendances (validation, testcontainers-postgresql, spring-security-test), configuration JaCoCo (minimum 80% lignes sur cinema/**).
- docker-compose.yml : valeur par défaut JWT_SECRET en base64, variables CINEMA_BOOTSTRAP_* ajoutées.
- .github/workflows/ci.yml : duration timeout augmenté, cache gradle, tests unit + integration, gate couverture et upload du rapport.
- Classes existantes étendues : controllers/services/mappers/repositories modifiés pour supporter validation, JWT, rôles, upserts.
- GlobalExceptionHandler enrichi : gestion des validations Bean, conversions d'erreurs DB en 409, etc.
- application.properties : propriétés JWT, bootstrap et paramètres JPA/Flyway.

## Points fonctionnels clés
- JWT : tokens HS256 contenant subject=email, role et userId; JwtService expose extractors et la durée d'expiration.
- Sécurité : règles HttpSecurity davantage détaillées ; annotations @PreAuthorize sur contrôleurs ; rôles CLIENT / EMPLOYEE / MANAGER.
- PUT upsert : PUT pour movies/projections/reservations/rooms supporte création (id null) et update (id donné).
- Validation : DTOs annotés (jakarta.validation) et GlobalExceptionHandler formate les erreurs de validation.
- Contraintes en base renforcées (V2, V3) : index, unicités, checks, trigger pour projection_id, contrainte unique seat per projection (prévenir double-booking).
- Manager bootstrap : ApplicationRunner qui crée le premier MANAGER si activé par variable d'environnement.
- Tests : large suite d'IT basés sur Testcontainers (Postgres) et tests unitaires/mocks.

---

Si vous voulez, je peux :
1) ajouter ce fichier dans le dépôt (à la racine ou dans un dossier docs/) — commit automatique ;
2) créer un fichier .md ou .txt dans le repo et vous donner le lien vers le fichier créé ;
3) ou vous fournir le fichier en téléchargement ici (contenu brut) pour que vous le copiez.

