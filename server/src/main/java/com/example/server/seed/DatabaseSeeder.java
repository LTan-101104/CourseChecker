package com.example.server.seed;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.server.model.AndRequirement;
import com.example.server.model.CompletedCourse;
import com.example.server.model.Course;
import com.example.server.model.CourseRequirement;
import com.example.server.model.OrRequirement;
import com.example.server.model.Requirement;
import com.example.server.model.User;
import com.example.server.repository.CompletedCourseRepository;
import com.example.server.repository.CourseRepository;
import com.example.server.repository.UserRepository;

/**
 * Seeds the database on application startup using data from a {@link CourseDataProvider}.
 *
 * <p>This class is responsible ONLY for the database insertion logic.
 * The actual data payload is provided by the injected {@link CourseDataProvider},
 * making the data source easily swappable (mock → web crawler).
 *
 * <p>The seeder is idempotent: it checks if data already exists before inserting.
 */
@Component
@Profile("dev")
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final CourseDataProvider dataProvider;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CompletedCourseRepository completedCourseRepository;

    public DatabaseSeeder(CourseDataProvider dataProvider,
                          CourseRepository courseRepository,
                          UserRepository userRepository,
                          CompletedCourseRepository completedCourseRepository) {
        this.dataProvider = dataProvider;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.completedCourseRepository = completedCourseRepository;
    }

    @Override
    public void run(String... args) {
        seedCourses();
        seedUsers();
        seedCompletedCourses();
    }

    // ── Course Seeding ──────────────────────────────────────

    private void seedCourses() {
        List<CourseDefinition> definitions = dataProvider.getCourseDefinitions();

        for (CourseDefinition def : definitions) {
            if (courseRepository.existsByCourseCode(def.getCourseCode())) {
                log.info("⏭  Course {} already exists, skipping.", def.getCourseCode());
                continue;
            }

            Course course = new Course(
                def.getCourseCode(),
                def.getTitle(),
                def.getCredits(),
                def.getDescription()
            );

            if (def.getPrerequisite() != null) {
                Requirement prereqTree = buildRequirementTree(def.getPrerequisite());
                course.setPrerequisite(prereqTree);
            }

            courseRepository.save(course);
            log.info("✅ Seeded course: {}", def.getCourseCode());
        }
    }

    /**
     * Recursively converts a {@link RequirementDefinition} tree (DTO)
     * into the corresponding JPA entity tree.
     */
    private Requirement buildRequirementTree(RequirementDefinition def) {
        return switch (def.getType()) {
            case COURSE -> new CourseRequirement(def.getCourseCode());

            case AND -> {
                AndRequirement and = new AndRequirement();
                for (RequirementDefinition child : def.getChildren()) {
                    and.addChild(buildRequirementTree(child));
                }
                yield and;
            }

            case OR -> {
                OrRequirement or = new OrRequirement();
                for (RequirementDefinition child : def.getChildren()) {
                    or.addChild(buildRequirementTree(child));
                }
                yield or;
            }
        };
    }

    // ── User Seeding ────────────────────────────────────────

    private void seedUsers() {
        List<CourseDataProvider.UserDefinition> definitions = dataProvider.getUserDefinitions();

        for (CourseDataProvider.UserDefinition def : definitions) {
            User user = userRepository.findByStudentId(def.studentId()).orElse(null);
            boolean isNewUser = user == null;

            if (isNewUser) {
                user = new User(
                    def.studentId(),
                    def.displayName(),
                    def.email(),
                    def.passwordHash()
                );
            } else {
                if (isNullOrBlank(user.getDisplayName()) && !isNullOrBlank(def.displayName())) {
                    user.setDisplayName(def.displayName());
                }
                if (isNullOrBlank(user.getEmail()) && !isNullOrBlank(def.email())) {
                    user.setEmail(def.email());
                }
            }

            // Preserve any real password hash if the seed payload leaves it null.
            if (def.passwordHash() != null || user.getPasswordHash() == null) {
                user.setPasswordHash(def.passwordHash());
            }

            userRepository.save(user);
            log.info("✅ Seeded user: {}", def.studentId());
        }
    }

    private boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    // ── Transcript Seeding ──────────────────────────────────

    private void seedCompletedCourses() {
        List<CourseDataProvider.CompletedCourseDefinition> definitions =
            dataProvider.getCompletedCourseDefinitions();

        java.util.Set<String> studentIds = new java.util.LinkedHashSet<>();
        for (CourseDataProvider.CompletedCourseDefinition def : definitions) {
            studentIds.add(def.studentId());
        }

        java.util.Map<String, User> usersByStudentId = new java.util.HashMap<>();
        for (User user : userRepository.findAll()) {
            if (studentIds.contains(user.getStudentId())) {
                usersByStudentId.put(user.getStudentId(), user);
            }
        }

        java.util.Set<String> missingStudentIds = new java.util.LinkedHashSet<>();
        for (CourseDataProvider.CompletedCourseDefinition def : definitions) {
            if (!usersByStudentId.containsKey(def.studentId())) {
                missingStudentIds.add(def.studentId());
            }
        }

        if (!missingStudentIds.isEmpty()) {
            throw new IllegalStateException(
                "Cannot seed transcript entries for missing users. Add these studentIds to "
                    + "getUserDefinitions() or remove/fix the corresponding transcript entries "
                    + "in getCompletedCourseDefinitions(): " + missingStudentIds
            );
        }

        for (CourseDataProvider.CompletedCourseDefinition def : definitions) {
            if (completedCourseRepository.existsByUserStudentIdAndCourseCode(
                    def.studentId(), def.courseCode())) {
                log.info("⏭  Transcript entry {}/{} already exists, skipping.",
                    def.studentId(), def.courseCode());
                continue;
            }

            if (missingStudentIds.contains(def.studentId())) {
                log.warn(
                    "⏭  Skipping transcript entry {}/{} because user {} is missing.",
                    def.studentId(), def.courseCode(), def.studentId()
                );
                continue;
            }

            User user = usersByStudentId.get(def.studentId());

            CompletedCourse cc = new CompletedCourse(
                user,
                def.courseCode(),
                def.grade(),
                def.semester()
            );

            completedCourseRepository.save(cc);
            log.info("✅ Seeded transcript: {} completed {}",
                def.studentId(), def.courseCode());
        }
    }
}
