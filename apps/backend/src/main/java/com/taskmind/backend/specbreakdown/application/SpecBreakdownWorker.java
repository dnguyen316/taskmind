package com.taskmind.backend.specbreakdown.application;
import org.springframework.stereotype.Component;
@Component public class SpecBreakdownWorker { private final SpecBreakdownApplicationService service; public SpecBreakdownWorker(SpecBreakdownApplicationService service){this.service=service;} }
