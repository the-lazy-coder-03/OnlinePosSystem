package org.example.onlinepossystem.notification.email.resend;

import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

@FunctionalInterface
interface ResendClient {
    CreateEmailResponse send(CreateEmailOptions request) throws ResendException;
}
