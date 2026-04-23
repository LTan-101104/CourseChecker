package com.example.server.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.server.model.CompletedCourse;
import com.example.server.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CompletedCourseRepositoryTest {

    @Autowired
    private CompletedCourseRepository completedCourseRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void duplicateCompletedCourseIsRejectedForTheSameUser() {
        User alice = userRepository.saveAndFlush(
            new User("A10001", "Alice", "alice@example.com", "hash-1")
        );

        completedCourseRepository.saveAndFlush(
            new CompletedCourse(alice, "COMPSCI 187", "A", "Fall 2024")
        );

        assertThatThrownBy(() -> completedCourseRepository.saveAndFlush(
            new CompletedCourse(alice, "COMPSCI 187", "B", "Spring 2025")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameCompletedCourseCodeCanExistForDifferentUsers() {
        User alice = userRepository.saveAndFlush(
            new User("A10001", "Alice", "alice@example.com", "hash-1")
        );
        User bob = userRepository.saveAndFlush(
            new User("A10002", "Bob", "bob@example.com", "hash-2")
        );

        completedCourseRepository.saveAndFlush(
            new CompletedCourse(alice, "COMPSCI 187", "A", "Fall 2024")
        );
        CompletedCourse bobCourse = completedCourseRepository.saveAndFlush(
            new CompletedCourse(bob, "COMPSCI 187", "A-", "Fall 2024")
        );

        assertThat(completedCourseRepository.existsByUserIdAndCourseCode(alice.getId(), "COMPSCI 187"))
            .isTrue();
        assertThat(completedCourseRepository.existsByUserStudentIdAndCourseCode("A10002", "COMPSCI 187"))
            .isTrue();
        assertThat(bobCourse.getId()).isNotNull();
    }

    @Test
    void ownershipQueriesOnlyReturnRowsForTheMatchingUserAndKeepSortedListsScoped() {
        User alice = userRepository.saveAndFlush(
            new User("A10001", "Alice", "alice@example.com", "hash-1")
        );
        User bob = userRepository.saveAndFlush(
            new User("A10002", "Bob", "bob@example.com", "hash-2")
        );

        CompletedCourse aliceLater = completedCourseRepository.saveAndFlush(
            new CompletedCourse(alice, "COMPSCI 220", "A", "Spring 2025")
        );
        completedCourseRepository.saveAndFlush(
            new CompletedCourse(alice, "COMPSCI 187", "A-", "Fall 2024")
        );
        completedCourseRepository.saveAndFlush(
            new CompletedCourse(bob, "MATH 132", "B+", "Fall 2024")
        );

        assertThat(completedCourseRepository.findByIdAndUserId(aliceLater.getId(), alice.getId()))
            .isPresent()
            .get()
            .extracting(CompletedCourse::getCourseCode)
            .isEqualTo("COMPSCI 220");
        assertThat(completedCourseRepository.findByIdAndUserId(aliceLater.getId(), bob.getId()))
            .isEmpty();

        assertThat(completedCourseRepository.findByUserIdOrderByCourseCodeAsc(alice.getId()))
            .extracting(CompletedCourse::getCourseCode)
            .containsExactly("COMPSCI 187", "COMPSCI 220");
        assertThat(completedCourseRepository.findByUserIdOrderByCourseCodeAsc(bob.getId()))
            .extracting(CompletedCourse::getCourseCode)
            .containsExactly("MATH 132");
    }
}
