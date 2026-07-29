package com.brinza.notary.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsyncEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private AsyncEmailSender sender() {
        return new AsyncEmailSender(mailSender);
    }

    @Test
    void sendsMessageThroughMailSender() {
        SimpleMailMessage message = new SimpleMailMessage();

        sender().sendAsync(message, 1L);

        verify(mailSender).send(message);
    }

    @Test
    void swallowsMailExceptionWithoutPropagating() {
        SimpleMailMessage message = new SimpleMailMessage();
        doThrow(new MailSendException("smtp down")).when(mailSender).send(message);

        assertThatCode(() -> sender().sendAsync(message, 1L)).doesNotThrowAnyException();
    }
}
