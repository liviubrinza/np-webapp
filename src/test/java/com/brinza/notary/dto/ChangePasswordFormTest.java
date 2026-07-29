package com.brinza.notary.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordFormTest {

    @Test
    void nullNewPasswordIsConsideredMatching() {
        ChangePasswordForm form = new ChangePasswordForm();
        form.setNewPassword(null);
        form.setConfirmNewPassword("anything");

        assertThat(form.isConfirmationMatching()).isTrue();
    }

    @Test
    void matchingPasswordsAreConfirmed() {
        ChangePasswordForm form = new ChangePasswordForm();
        form.setNewPassword("newpass123");
        form.setConfirmNewPassword("newpass123");

        assertThat(form.isConfirmationMatching()).isTrue();
    }

    @Test
    void mismatchedPasswordsAreRejected() {
        ChangePasswordForm form = new ChangePasswordForm();
        form.setNewPassword("newpass123");
        form.setConfirmNewPassword("different");

        assertThat(form.isConfirmationMatching()).isFalse();
    }
}
