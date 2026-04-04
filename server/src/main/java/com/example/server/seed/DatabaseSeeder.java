package com.example.server.seed;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.server.model.AndRequirement;
import com.example.server.model.CompletedCourse;
import com.example.server.model.Course;
import com.example.server.model.CourseRequirement;
import com.example.server.model.OrRequirement;
import com.example.server.model.Requirement;
import com.example.server.repository.CompletedCourseRepository;
import com.example.server.repository.CourseRepository;

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
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final CourseDataProvider dataProvider;
    private final CourseRepository courseRepository;
    private final CompletedCourseRepository completedCourseRepository;

    public DatabaseSeeder(CourseDataProvider dataProvider,
                          CourseRepository courseRepository,
                          CompletedCourseRepository completedCourseRepository) {
        this.dataProvider = dataProvider;
        this.courseRepository = courseRepository;
        this.completedCourseRepository = completedCourseRepository;
    }

    @Override
    public void run(String... args) {
        seedCourses();
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

    // ── Transcript Seeding ──────────────────────────────────

    private void seedCompletedCourses() {
        List<CourseDataProvider.CompletedCourseDefinition> definitions =
            dataProvider.getCompletedCourseDefinitions();

        for (CourseDataProvider.CompletedCourseDefinition def : definitions) {
            if (completedCourseRepository.existsByStudentIdAndCourseCode(
                    def.studentId(), def.courseCode())) {
                log.info("⏭  Transcript entry {}/{} already exists, skipping.",
                    def.studentId(), def.courseCode());
                continue;
            }

            CompletedCourse cc = new CompletedCourse(
                def.studentId(),
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
