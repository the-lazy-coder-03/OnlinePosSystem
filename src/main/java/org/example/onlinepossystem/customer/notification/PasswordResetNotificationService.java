package org.example.onlinepossystem.customer.notification;

import org.example.onlinepossystem.notification.email.EmailMessage;
import org.example.onlinepossystem.notification.email.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetNotificationService implements PasswordResetNotifier {

    private final EmailSender emailSender;
    private final String appBaseUrl;

    public PasswordResetNotificationService(
            EmailSender emailSender,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl
    ) {
        this.emailSender = emailSender;
        this.appBaseUrl = normalizeBaseUrl(appBaseUrl);
    }

    @Override
    public void sendResetLink(String email, String token) {
        String resetUrl = UriComponentsBuilder.fromUriString(appBaseUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();

        emailSender.send(new EmailMessage(
                email,
                "Reset your Pete's Pizza password",
                htmlContent(resetUrl),
                textContent(resetUrl)
        ));
    }

    private String htmlContent(String resetUrl) {
        String safeResetUrl = HtmlUtils.htmlEscape(resetUrl);
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Reset your Pete's Pizza password</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f3f5f8;color:#202938;font-family:Arial,Helvetica,sans-serif;">
                  <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;">Set a new password for your Pete's Pizza account. Your link expires in 30 minutes.</div>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f3f5f8;">
                    <tr><td align="center" style="padding:32px 16px;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;">
                        <tr><td style="padding:0 8px 24px;color:#0052cc;font-size:24px;font-weight:bold;">Pete's Pizza</td></tr>
                        <tr><td style="padding:32px 24px;background-color:#ffffff;border-top:4px solid #0052cc;border-radius:8px;">
                          <p style="margin:0 0 12px;color:#596579;font-size:12px;font-weight:bold;">ACCOUNT SECURITY</p>
                          <h1 style="margin:0 0 20px;font-size:28px;line-height:1.25;color:#202938;">Let's reset your password</h1>
                          <p style="margin:0 0 24px;font-size:16px;line-height:1.6;">We received a request to reset the password for your Pete's Pizza account. Select the button below to choose a new one.</p>
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr><td align="center" bgcolor="#0052cc" style="border-radius:6px;mso-padding-alt:16px 24px;">
                              <a href="%s" style="display:inline-block;padding:16px 24px;border:1px solid #0052cc;border-radius:6px;color:#ffffff;font-size:16px;font-weight:bold;text-decoration:none;">Reset password</a>
                            </td></tr>
                          </table>
                          <p style="margin:16px 0 28px;color:#596579;font-size:14px;line-height:1.5;">This link expires in <strong>30 minutes</strong>. Keep it private.</p>
                          <p style="margin:0 0 12px;padding-top:24px;border-top:1px solid #e4e8ee;font-size:14px;line-height:1.6;"><strong>Didn't request this?</strong><br>You can safely ignore this email. Your password will stay the same.</p>
                          <p style="margin:20px 0 8px;color:#596579;font-size:13px;line-height:1.6;">Button not working? Copy and paste this link into your browser:</p>
                          <p style="margin:0;font-size:13px;line-height:1.6;word-break:break-all;overflow-wrap:anywhere;"><a href="%s" style="color:#0052cc;text-decoration:underline;word-break:break-all;">%s</a></p>
                        </td></tr>
                        <tr><td align="center" style="padding:24px 12px;color:#596579;font-size:12px;line-height:1.6;">Pete's Pizza &middot; Account security<br>This email was sent in response to a password reset request.</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeResetUrl, safeResetUrl, safeResetUrl);
    }

    private String textContent(String resetUrl) {
        return """
                Pete's Pizza
                Let's reset your password

                We received a request to reset the password for your Pete's Pizza account.

                Use this link within 30 minutes:
                %s

                Keep this link private.

                Didn't request this? You can safely ignore this email. Your password will stay the same.
                """.formatted(resetUrl);
    }

    private String normalizeBaseUrl(String value) {
        String fallback = "http://localhost:8080";
        String normalized = StringUtils.hasText(value) ? value.trim() : fallback;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
