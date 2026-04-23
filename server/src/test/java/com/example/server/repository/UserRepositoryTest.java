package com.example.server.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.example.server.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void emailAndStudentIdChecksRespectCaseInsensitiveLookupsAndSelfExclusion() {
        User alice = userRepository.saveAndFlush(
            new User("A10001", "Alice", "alice@example.com", "hash-1")
        );
        User bob = userRepository.saveAndFlush(
            new User("A10002", "Bob", "bob@example.com", "hash-2")
        );

        assertThat(userRepository.findByEmailIgnoreCase("ALICE@EXAMPLE.COM"))
            .get()
            .extracting(User::getId)
            .isEqualTo(alice.getId());
        assertThat(userRepository.existsByEmailIgnoreCase("ALICE@EXAMPLE.COM")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCaseAndIdNot("ALICE@EXAMPLE.COM", alice.getId()))
            .isFalse();
        assertThat(userRepository.existsByEmailIgnoreCaseAndIdNot("ALICE@EXAMPLE.COM", bob.getId()))
            .isTrue();

        assertThat(userRepository.existsByStudentIdAndIdNot("A10001", alice.getId())).isFalse();
        assertThat(userRepository.existsByStudentIdAndIdNot("A10001", bob.getId())).isTrue();
    }
}
