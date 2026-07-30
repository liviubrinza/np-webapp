package com.brinza.notary.dto;

import com.brinza.notary.domain.AdminRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUserFormTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void validFormHasNoViolations() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("titi");
        form.setPassword("secret");
        form.setFullName("Titi Full Name");
        form.setRole(AdminRole.TECHNICIAN);

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void blankUsernameIsRejected() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername(" ");
        form.setFullName("Full Name");
        form.setRole(AdminRole.ADMIN);

        Set<ConstraintViolation<AdminUserForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void oversizedUsernameIsRejected() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("a".repeat(101));
        form.setFullName("Full Name");
        form.setRole(AdminRole.ADMIN);

        Set<ConstraintViolation<AdminUserForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void blankFullNameIsRejected() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("titi");
        form.setFullName(" ");
        form.setRole(AdminRole.ADMIN);

        Set<ConstraintViolation<AdminUserForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    void oversizedFullNameIsRejected() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("titi");
        form.setFullName("a".repeat(256));
        form.setRole(AdminRole.ADMIN);

        Set<ConstraintViolation<AdminUserForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    void nullRoleIsRejected() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("titi");
        form.setFullName("Full Name");
        form.setRole(null);

        Set<ConstraintViolation<AdminUserForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("role"));
    }
}
