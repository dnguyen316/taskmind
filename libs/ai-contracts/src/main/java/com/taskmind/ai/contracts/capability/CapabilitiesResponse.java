package com.taskmind.ai.contracts.capability;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/** Core-safe list of Nova capabilities exposed through the authenticated facade. */
@JsonPropertyOrder({"capabilities"})
public record CapabilitiesResponse(List<CapabilityDescriptor> capabilities) {}
