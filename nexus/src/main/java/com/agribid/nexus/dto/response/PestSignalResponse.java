package com.agribid.nexus.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PestSignalResponse(
    String district,
    String categoryCode,
    List<PestSignalItem> signals,
    String disclaimer
) {
    public record PestSignalItem(String pestCode, int reportCount, LocalDate mostRecentWeek) {}
}
