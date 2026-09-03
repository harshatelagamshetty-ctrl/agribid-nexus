package com.agribid.nexus.controller;

import com.agribid.nexus.domain.crop.Category;
import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.regional.RegionalSignal;
import com.agribid.nexus.domain.user.User;
import com.agribid.nexus.repository.CategoryRepository;
import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.repository.RegionalSignalRepository;
import com.agribid.nexus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

/**
 * Implements the widely-used USSD aggregator callback contract
 * (sessionId, phoneNumber, text — the same shape used by Africa's
 * Talking and most Indian telecom USSD gateways): every request
 * carries the FULL accumulated navigation path so far as one
 * asterisk-delimited string, e.g. "2*1*Nashik" means "chose menu 2,
 * then sub-option 1, then typed Nashik." This means the gateway
 * itself owns session state — this controller needs none of its
 * own, which is the entire reason USSD backends can be this simple.
 *
 * Honest limitation: this implements the CONTRACT SHAPE correctly
 * and completely, but has not been tested against a specific real
 * Indian telecom aggregator's exact field names, which vary slightly
 * between providers. Wiring to a specific real aggregator is a
 * configuration/mapping step, not a redesign of this logic.
 */
@RestController
@RequestMapping("/api/v1/ussd")
@RequiredArgsConstructor
public class UssdController {

    private static final List<String> MENU_CROPS = List.of("WHEAT", "TOMATO", "RICE", "ONION", "POTATO");

    private final UserRepository userRepository;
    private final CropLotRepository cropLotRepository;
    private final CategoryRepository categoryRepository;
    private final RegionalSignalRepository regionalSignalRepository;

    @PostMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdCallback(
            @RequestParam String sessionId,
            @RequestParam String phoneNumber,
            @RequestParam(defaultValue = "") String text) {

        String[] path = text.isBlank() ? new String[0] : text.split("\\*");
        String response = routeMenu(path, phoneNumber);
        return ResponseEntity.ok(response);
    }

    private String routeMenu(String[] path, String phoneNumber) {
        if (path.length == 0) {
            return con("Welcome to AgriBid Nexus\n1. My latest listing status\n2. Regional price check\n3. Link this number");
        }

        return switch (path[0]) {
            case "1" -> handleListingStatus(phoneNumber);
            case "2" -> handleRegionalPrice(path);
            case "3" -> handleLinkPhone(path, phoneNumber);
            default -> end("Invalid selection.");
        };
    }

    private String handleListingStatus(String phoneNumber) {
        Optional<User> user = userRepository.findByPhoneNumber(phoneNumber);
        if (user.isEmpty()) {
            return end("This number isn't linked to an account yet. Dial in and choose option 3 to link it first.");
        }
        var page = cropLotRepository.findByOwnerId(user.get().getId(),
                org.springframework.data.domain.PageRequest.of(0, 1,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        if (page.isEmpty()) {
            return end("No crop lots found on your account yet.");
        }
        CropLot latest = page.getContent().get(0);
        return end("Latest lot #" + latest.getId() + ": " + latest.getCategory().getName()
                + ", status " + latest.getStatus()
                + (latest.getQualityGrade() != null ? ", grade " + latest.getQualityGrade().getGradeLabel() : ""));
    }

    private String handleRegionalPrice(String[] path) {
        if (path.length == 1) {
            StringBuilder menu = new StringBuilder("Select crop:\n");
            for (int i = 0; i < MENU_CROPS.size(); i++) {
                menu.append(i + 1).append(". ").append(MENU_CROPS.get(i)).append("\n");
            }
            return con(menu.toString().trim());
        }
        if (path.length == 2) {
            return con("Enter your district name:");
        }
        // path = ["2", "<crop index>", "<district>"]
        int cropIndex;
        try {
            cropIndex = Integer.parseInt(path[1]) - 1;
        } catch (NumberFormatException e) {
            return end("Invalid crop selection.");
        }
        if (cropIndex < 0 || cropIndex >= MENU_CROPS.size()) {
            return end("Invalid crop selection.");
        }
        String cropCode = MENU_CROPS.get(cropIndex);
        String district = path[2];

        Optional<Category> category = categoryRepository.findByCode(cropCode);
        if (category.isEmpty()) {
            return end("Crop category not found.");
        }
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Optional<RegionalSignal> signal = regionalSignalRepository
                .findByDistrictAndCategoryIdAndWeekStart(district, category.get().getId(), weekStart);

        if (signal.isEmpty() || signal.get().getAvgSettledPricePerKg() == null) {
            return end("No verified settled price on record yet for " + cropCode + " in " + district + " this week.");
        }
        return end(cropCode + " in " + district + " this week: avg Rs." + signal.get().getAvgSettledPricePerKg()
                + "/kg from " + signal.get().getSettledTransactionCount() + " verified sale(s).");
    }

    private String handleLinkPhone(String[] path, String phoneNumber) {
        if (path.length == 1) {
            return con("Enter your registered account email:");
        }
        String email = path[1];
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return end("No account found for that email.");
        }
        user.get().setPhoneNumber(phoneNumber);
        userRepository.save(user.get());
        return end("This number is now linked to " + email + ".");
    }

    /** CON = show this text and wait for the next input in the same session. */
    private String con(String text) {
        return "CON " + text;
    }

    /** END = show this text and terminate the session. */
    private String end(String text) {
        return "END " + text;
    }
}
