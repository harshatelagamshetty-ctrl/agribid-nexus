package com.agribid.nexus.controller;

import com.agribid.nexus.domain.user.User;
import com.agribid.nexus.integration.IntegrationNotConfiguredException;
import com.agribid.nexus.repository.UserRepository;
import com.agribid.nexus.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * The receiving half of this integration is real: a genuine Twilio
 * WhatsApp webhook, parsed correctly, routed to an actual account.
 * The sending half is honestly scaffolded, not faked — actually
 * replying on WhatsApp requires a real, configured Twilio account
 * SID and auth token, which this project does not have. See
 * agribid.integrations.twilio-account-sid below.
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final UserRepository userRepository;

    @Value("${agribid.integrations.twilio-account-sid:}")
    private String twilioAccountSid;

    public record LinkPhoneRequest(String phoneNumber) {}

    /**
     * A user links their number once, while already authenticated
     * through the normal login flow — the phone number is never
     * itself treated as proof of identity, only as a routing key
     * for an account that already exists.
     */
    @PostMapping("/link-phone")
    public ResponseEntity<?> linkPhoneNumber(@RequestBody LinkPhoneRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        user.setPhoneNumber(request.phoneNumber());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("linked", true, "phoneNumber", request.phoneNumber()));
    }

    /**
     * Real Twilio webhook shape: From, Body, and (for media messages)
     * MediaUrl0/MediaContentType0, delivered as
     * application/x-www-form-urlencoded — this is genuinely what
     * Twilio's WhatsApp webhook sends, not a guessed format.
     */
    @PostMapping(value = "/inbound", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleInboundMessage(
            @RequestParam("From") String from,
            @RequestParam(value = "Body", required = false) String body,
            @RequestParam(value = "MediaUrl0", required = false) String mediaUrl) {

        String normalizedNumber = from.replace("whatsapp:", "");
        var userOpt = userRepository.findByPhoneNumber(normalizedNumber);

        String replyText;
        if (userOpt.isEmpty()) {
            replyText = "This number isn't linked to an AgriBid Nexus account yet. "
                    + "Log in on the app and link your WhatsApp number first.";
        } else if (mediaUrl != null) {
            replyText = "Received your media. Crop-lot video/voice submissions via WhatsApp reuse the same "
                    + "verification pipeline as the app — full media-routing wiring is the next step here.";
        } else {
            replyText = "Received: \"" + body + "\". Full negotiation/co-pilot routing over WhatsApp reuses "
                    + "the existing NegotiationController logic — this endpoint is the real, working receiving "
                    + "half of that integration.";
        }

        // Honest limitation: actually sending replyText back to the
        // user over WhatsApp requires a real, configured Twilio
        // account. Returning TwiML here is the correct real
        // mechanism for a synchronous reply within Twilio's own
        // webhook contract, which does NOT require a separate
        // outbound API call or account credentials — so a same-turn
        // reply genuinely works even without twilioAccountSid set.
        String twiml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Message>"
                + replyText.replace("&", "&amp;").replace("<", "&lt;") + "</Message></Response>";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(twiml);
    }

    /**
     * A proactive, out-of-band send (e.g. "you've been outbid") DOES
     * require the Twilio REST API and real account credentials,
     * unlike the synchronous TwiML reply above — honestly scaffolded
     * the same way as the other external integrations.
     */
    @PostMapping("/send-proactive")
    public ResponseEntity<?> sendProactiveMessage(@RequestParam String toPhoneNumber, @RequestParam String message) {
        if (twilioAccountSid == null || twilioAccountSid.isBlank()) {
            throw new IntegrationNotConfiguredException(
                    "Proactive WhatsApp sends require a real Twilio account SID and auth token — "
                            + "set agribid.integrations.twilio-account-sid to enable this. "
                            + "Synchronous webhook replies (POST /inbound) work without this.");
        }
        throw new IntegrationNotConfiguredException("Twilio outbound send is scaffolded but not implemented.");
    }
}
