package com.agribid.nexus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled processing so scheduler.AuctionCloseScheduler's
 * job actually runs. Kept as its own trivial config class (rather
 * than annotating the main application class) so the "scheduling is
 * on" decision is discoverable in config/ alongside every other
 * cross-cutting toggle, instead of hidden on the entry point.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
